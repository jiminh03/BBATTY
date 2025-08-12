import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  Alert,
  TextInput,
  KeyboardAvoidingView,
  Platform,
  AppState,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import NetInfo from '@react-native-community/netinfo';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import type { RouteProp } from '@react-navigation/native';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { ChatMessage, MatchChatMessage, SystemMessage, MessageWithStatus, ExtendedConnectionStatus, ChatNotification } from '../../features/match-chat';
import type { ChatStackParamList } from '../../navigation/types';
import { useMessageQueue } from '../../features/match-chat/hooks/useMessageQueue';
import { useChatNotifications } from '../../features/match-chat/hooks/useChatNotifications';
import { ChatNotificationManager } from '../../features/match-chat/components/ChatNotification';
import { ConnectionStatusIndicator, SimpleConnectionStatus } from '../../features/match-chat/components/ConnectionStatus';
import { MessageStatusIndicator, SimpleMessageStatus } from '../../features/match-chat/components/MessageStatus';
import { getErrorMessage, logChatError } from '../../shared/utils/error';
import { useUserStore } from '../../entities/user/model/userStore';
import { useThemeColor } from '../../shared/team/ThemeContext';
import { styles } from './MatchChatRoomScreen.styles';

type NavigationProp = StackNavigationProp<ChatStackParamList>;
type RoutePropType = RouteProp<ChatStackParamList, 'MatchChatRoom'>;

export const MatchChatRoomScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<RoutePropType>();
  const insets = useSafeAreaInsets();
  const { room, websocketUrl, sessionToken } = route.params;
  const themeColor = useThemeColor();
  
  // 워치채팅 여부 확인
  const isWatchChat = websocketUrl.includes('/ws/watch-chat/') || (websocketUrl.includes('gameId=') && websocketUrl.includes('teamId='));
  
  const [currentMessage, setCurrentMessage] = useState('');
  const getCurrentUser = useUserStore((state) => state.getCurrentUser);
  const currentUser = getCurrentUser();
  const currentUserId = currentUser?.userId || 45;
  
  const scrollViewRef = useRef<ScrollView>(null);

  // 🔧 FIX 1: ref로 상태 관리하여 무한 루프 방지
  const connectionStateRef = useRef({
    isConnecting: false,
    isConnected: false,
    reconnectAttempts: 0,
    maxReconnectAttempts: 3,
    lastReconnectTime: 0,
    reconnectCooldown: 5000, // 5초 쿨다운
    isDestroyed: false,
    reconnectTimer: null as NodeJS.Timeout | null,
  });

  const [messages, setMessages] = useState<MessageWithStatus[]>([]);
  const [ws, setWs] = useState<WebSocket | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ExtendedConnectionStatus>('DISCONNECTED');
  const [sentMessages, setSentMessages] = useState<Set<string>>(new Set());
  const [appState, setAppState] = useState(AppState.currentState);
  const [networkConnected, setNetworkConnected] = useState(true);
  
  // 사용자 친화적 기능들
  const {
    notifications,
    dismissNotification,
    showConnectionNotification,
    showErrorNotification,
    showMessageNotification,
  } = useChatNotifications();

  const {
    pendingMessages,
    addMessageToQueue,
    retryMessage,
    removeMessage,
    flushQueue,
  } = useMessageQueue({
    onSendMessage: async (content: string) => {
      if (ws?.readyState === WebSocket.OPEN) {
        try {
          ws.send(content);
          return true;
        } catch (error) {
          console.error('WebSocket send error:', error);
          return false;
        }
      }
      return false;
    },
    isConnected: connectionStatus === 'CONNECTED',
    maxRetries: 3,
  });

  const scrollToBottom = useCallback(() => {
    setTimeout(() => {
      scrollViewRef.current?.scrollToEnd({ animated: true });
    }, 50);
  }, []);

  const addMessage = useCallback((message: ChatMessage, isMyMessage: boolean = false) => {
    setMessages(prev => {
      const isDuplicate = prev.some(m => 
        m.timestamp === message.timestamp && 
        m.content === message.content
      );
      
      if (isDuplicate) return prev;
      
      const markedMessage = {
        ...message,
        _isMyMessage: isMyMessage
      };
      
      const newMessages = [...prev, markedMessage];
      newMessages.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
      
      if (newMessages.length > 100) {
        return newMessages.slice(-100);
      }
      return newMessages;
    });
  }, []);

  // 🔧 FIX 2: 재연결 쿨다운 및 중복 방지 로직 추가
  const canReconnect = useCallback((): boolean => {
    const state = connectionStateRef.current;
    const now = Date.now();
    
    if (state.isDestroyed) return false;
    if (state.isConnecting) return false;
    if (state.isConnected) return false;
    if (state.reconnectAttempts >= state.maxReconnectAttempts) return false;
    if (now - state.lastReconnectTime < state.reconnectCooldown) return false;
    if (!networkConnected) return false;
    if (appState !== 'active') return false;
    
    return true;
  }, [networkConnected, appState]);

  const clearReconnectTimer = useCallback(() => {
    if (connectionStateRef.current.reconnectTimer) {
      clearTimeout(connectionStateRef.current.reconnectTimer);
      connectionStateRef.current.reconnectTimer = null;
    }
  }, []);

  const connectToWebSocket = useCallback(async () => {
    const state = connectionStateRef.current;
    
    // 🔧 FIX 3: 중복 연결 방지 강화
    if (state.isDestroyed) {
      console.log('📱 컴포넌트가 언마운트됨 - 연결 시도 중단');
      return;
    }

    if (!canReconnect()) {
      console.log('📱 재연결 조건 불충족 - 연결 시도 중단');
      return;
    }

    try {
      const netState = await NetInfo.fetch();
      if (!netState.isConnected) {
        console.log('🌐 네트워크 연결 없음 - WebSocket 연결 시도 중단');
        setConnectionStatus('ERROR');
        return;
      }

      // 기존 WebSocket 정리
      if (ws) {
        ws.close(1000, 'New connection attempt');
        setWs(null);
      }

      state.isConnecting = true;
      state.lastReconnectTime = Date.now();
      setConnectionStatus('CONNECTING');
      
      console.log(`📱 WebSocket 연결 시도 (${state.reconnectAttempts + 1}/${state.maxReconnectAttempts})`);
      
      let wsUrl = websocketUrl;
      if (wsUrl && wsUrl.includes('localhost')) {
        wsUrl = wsUrl.replace('localhost', '10.0.2.2');
      }
      
      if (wsUrl && !wsUrl.startsWith('ws://') && !wsUrl.startsWith('wss://')) {
        if (wsUrl.startsWith('http://')) {
          wsUrl = wsUrl.replace('http://', 'ws://');
        } else if (wsUrl.startsWith('https://')) {
          wsUrl = wsUrl.replace('https://', 'wss://');
        }
      }
      
      // Mock 처리 로직 (기존과 동일)
      if (wsUrl && wsUrl.includes('i13a403.p.ssafy.io:8083') && sessionToken && sessionToken.startsWith('mock_session_token')) {
        console.log('⚠️ 서버 WebSocket 연결 문제로 인한 임시 데모 모드');
        setTimeout(() => {
          if (!state.isDestroyed) {
            state.isConnecting = false;
            state.isConnected = true;
            state.reconnectAttempts = 0;
            setConnectionStatus('CONNECTED');
            console.log('📡 데모 모드: 연결 성공');
          }
        }, 1000);
        return;
      }

      if (!wsUrl) {
        console.error('웹소켓 URL이 없습니다!');
        state.isConnecting = false;
        setConnectionStatus('ERROR');
        return;
      }

      const websocket = new WebSocket(wsUrl);
      setWs(websocket);

      // 🔧 FIX 4: 연결 타임아웃 추가
      const connectionTimeout = setTimeout(() => {
        if (state.isConnecting) {
          console.log('📱 WebSocket 연결 타임아웃');
          websocket.close();
          state.isConnecting = false;
          setConnectionStatus('ERROR');
          scheduleReconnect();
        }
      }, 10000); // 10초 타임아웃

      websocket.onopen = () => {
        clearTimeout(connectionTimeout);
        if (state.isDestroyed) return;
        
        state.isConnecting = false;
        state.isConnected = true;
        state.reconnectAttempts = 0; // 성공 시 리셋
        setConnectionStatus('CONNECTED');
        console.log('📡 WebSocket 연결 성공');
        
        // 사용자에게 연결 성공 알림
        showConnectionNotification('CONNECTED');
        
        // 대기 중인 메시지들 재전송 시도
        await flushQueue();
        
        // 인증 데이터 전송 (기존 로직 유지)
        let authData;
        if (isWatchChat) {
          authData = {
            type: 'AUTH',
            gameId: room.gameId || '1258',
            teamId: currentUser?.teamId || 3,
            nickname: currentUser?.nickname || 'Anonymous',
            userId: currentUser?.userId || currentUserId
          };
        } else {
          authData = {
            type: 'AUTH',
            matchId: room.matchId,
            nickname: currentUser?.nickname || 'Anonymous',
            winRate: 75,
            profileImgUrl: currentUser?.profileImageURL || '',
            isWinFairy: false
          };
        }
        
        websocket.send(JSON.stringify(authData));
      };

      websocket.onmessage = (event) => {
        // 메시지 처리 로직 (기존과 동일)
        try {
          const messageData = JSON.parse(event.data);
          
          if (typeof messageData.timestamp === 'number') {
            messageData.timestamp = new Date(messageData.timestamp).toISOString();
          }
          
          const messageKey = `${messageData.content}_${messageData.timestamp}`;
          const isMyMessage = sentMessages.has(messageKey);
          
          if (messageData.messageType === 'CHAT' || messageData.type === 'CHAT_MESSAGE') {
            const content = messageData.content || '';
            
            const isAuthDataMessage = typeof content === 'string' && (
              content.includes('"matchId"') && content.includes('"winRate"') ||
              content.includes('"gameId"') && content.includes('"teamId"') ||
              content.includes('"isWinFairy"') ||
              content.includes('"profileImgUrl"')
            );
              
            if (!isAuthDataMessage) {
              addMessage(messageData, isMyMessage);
              
              if (isMyMessage) {
                setSentMessages(prev => {
                  const newSet = new Set(prev);
                  newSet.delete(messageKey);
                  return newSet;
                });
              }
            }
          } else if (
            messageData.messageType === 'USER_JOIN' ||
            messageData.messageType === 'USER_LEAVE'
          ) {
            addMessage(messageData, false);
          }
        } catch (error) {
          console.error('메시지 파싱 오류:', error);
        }
      };

      websocket.onclose = (event) => {
        clearTimeout(connectionTimeout);
        state.isConnecting = false;
        state.isConnected = false;
        setConnectionStatus('DISCONNECTED');
        console.log(`웹소켓 연결 종료: ${event.code} - ${event.reason}`);
        
        // 사용자에게 연결 끊어짐 알림 (정상 종료가 아닌 경우만)
        if (event.code !== 1000 && !state.isDestroyed) {
          const error = getErrorMessage({
            type: 'close',
            code: event.code,
            reason: event.reason
          });
          logChatError(error, { code: event.code, reason: event.reason });
          showErrorNotification(error);
          
          scheduleReconnect();
        }
      };

      websocket.onerror = (error) => {
        clearTimeout(connectionTimeout);
        state.isConnecting = false;
        state.isConnected = false;
        setConnectionStatus('ERROR');
        console.error('웹소켓 오류:', error);
        
        if (!state.isDestroyed) {
          const chatError = getErrorMessage({
            type: 'CONNECTION_ERROR',
            message: 'WebSocket connection error'
          });
          logChatError(chatError, { originalError: error });
          showErrorNotification(chatError, () => {
            // 사용자가 재시도 버튼을 클릭하면 즉시 재연결 시도
            connectToWebSocket();
          });
          
          scheduleReconnect();
        }
      };

    } catch (error) {
      state.isConnecting = false;
      setConnectionStatus('ERROR');
      console.error('웹소켓 연결 오류:', error);
      scheduleReconnect();
    }
  }, [websocketUrl, sessionToken, canReconnect, ws, isWatchChat, room, currentUser, currentUserId, sentMessages, addMessage]);

  // 🔧 FIX 6: 재연결 스케줄링 함수 분리
  const scheduleReconnect = useCallback(() => {
    const state = connectionStateRef.current;
    
    if (!canReconnect()) {
      return;
    }

    clearReconnectTimer();
    
    state.reconnectAttempts++;
    const backoffDelay = Math.min(Math.pow(2, state.reconnectAttempts - 1) * 1000, 10000);
    
    console.log(`🔄 재연결 예약: ${state.reconnectAttempts}/${state.maxReconnectAttempts} (${backoffDelay}ms 후)`);
    
    // 재연결 시도 알림 표시
    setConnectionStatus('RECONNECTING');
    showConnectionNotification('RECONNECTING', state.reconnectAttempts, state.maxReconnectAttempts);
    
    state.reconnectTimer = setTimeout(() => {
      if (!state.isDestroyed && canReconnect()) {
        setConnectionStatus('CONNECTING');
        connectToWebSocket();
      }
    }, backoffDelay);
  }, [canReconnect, connectToWebSocket, clearReconnectTimer, showConnectionNotification]);

  const disconnect = useCallback(() => {
    console.log('📱 WebSocket 연결 해제');
    
    const state = connectionStateRef.current;
    state.isDestroyed = true;
    state.isConnecting = false;
    state.isConnected = false;
    
    clearReconnectTimer();
    
    if (ws) {
      ws.close(1000, 'Client disconnect');
      setWs(null);
    }
    
    setConnectionStatus('DISCONNECTED');
    setSentMessages(new Set());
  }, [ws, clearReconnectTimer]);

  const sendMessage = useCallback(async () => {
    if (!currentMessage.trim()) return;

    const messageContent = currentMessage.trim();
    
    // 즉시 입력 필드 클리어 (사용자 경험 개선)
    setCurrentMessage('');
    
    try {
      // 메시지 큐에 추가 (자동으로 전송 시도)
      const messageId = await addMessageToQueue(messageContent);
      
      // 로컬에서 즉시 메시지 표시 (낙관적 업데이트)
      const timestamp = new Date().toISOString();
      const localMessage: MessageWithStatus = {
        messageType: 'CHAT',
        roomId: room.matchId || '',
        userId: currentUser?.userId?.toString() || currentUserId.toString(),
        nickname: currentUser?.nickname || 'Anonymous',
        content: messageContent,
        timestamp,
        id: messageId,
        status: 'sending',
        _isMyMessage: true,
      };
      
      addMessage(localMessage, true);
      
      console.log('메시지 큐에 추가:', messageContent);
    } catch (error) {
      console.error('메시지 전송 오류:', error);
      const chatError = getErrorMessage({
        type: 'MESSAGE_SEND',
        message: error instanceof Error ? error.message : '메시지 전송 실패'
      });
      logChatError(chatError, { content: messageContent });
      showErrorNotification(chatError);
      
      // 실패 시 입력 필드 복원
      setCurrentMessage(messageContent);
    }
  }, [currentMessage, addMessageToQueue, addMessage, room, currentUser, currentUserId, showErrorNotification]);

  // 🔧 FIX 7: useEffect 의존성 배열 최적화
  
  // 메시지 스크롤
  useEffect(() => {
    if (messages.length > 0) {
      scrollToBottom();
    }
  }, [messages, scrollToBottom]);

  // 초기 연결 (한 번만)
  useEffect(() => {
    console.log('📱 MatchChatRoomScreen 마운트됨 - WebSocket 연결 시작');
    
    const timer = setTimeout(() => {
      connectToWebSocket();
    }, 500);
    
    return () => {
      clearTimeout(timer);
    };
  }, []); // 의존성 배열 비움

  // AppState 이벤트 리스너 (한 번만 등록)
  useEffect(() => {
    const handleAppStateChange = (nextAppState: string) => {
      console.log('📱 앱 상태 변화:', appState, '→', nextAppState);
      
      const prevAppState = appState;
      setAppState(nextAppState);
      
      // background -> active 복귀 시에만 재연결 시도
      if (prevAppState.match(/inactive|background/) && nextAppState === 'active') {
        console.log('📱 백그라운드에서 복귀 - WebSocket 상태 확인');
        setTimeout(() => {
          if (canReconnect()) {
            console.log('📱 백그라운드 복귀 시 WebSocket 재연결 시도');
            connectToWebSocket();
          }
        }, 1000);
      }
    };

    const subscription = AppState.addEventListener('change', handleAppStateChange);
    return () => subscription?.remove();
  }, [appState, canReconnect, connectToWebSocket]); // 안정적인 의존성만 포함

  // 네비게이션 이벤트 (한 번만 등록)
  useEffect(() => {
    const unsubscribeBeforeRemove = navigation.addListener('beforeRemove', () => {
      console.log('📱 화면 제거 직전 - WebSocket 연결 해제');
      disconnect();
    });

    const unsubscribeBlur = navigation.addListener('blur', () => {
      console.log('📱 MatchChatRoom 화면 블러됨 - WebSocket 연결 해제');
      disconnect();
    });

    return () => {
      unsubscribeBeforeRemove();
      unsubscribeBlur();
    };
  }, [navigation, disconnect]);

  // 네트워크 상태 모니터링 (한 번만 등록)
  useEffect(() => {
    const unsubscribe = NetInfo.addEventListener(state => {
      const isConnected = state.isConnected ?? false;
      console.log('🌐 네트워크 상태 변화:', { isConnected, type: state.type });
      
      const prevNetworkConnected = networkConnected;
      setNetworkConnected(isConnected);
      
      if (!isConnected) {
        // 오프라인 상태 알림
        setConnectionStatus('OFFLINE');
        showConnectionNotification('OFFLINE');
        
        if (ws) {
          console.log('🌐 네트워크 연결 끊어짐 - WebSocket 연결 해제');
          ws.close(1000, 'Network disconnected');
        }
      } else if (isConnected && !prevNetworkConnected && appState === 'active') {
        // 온라인 복구 시 알림 및 재연결
        console.log('🌐 네트워크 다시 연결됨 - WebSocket 재연결 시도');
        
        setTimeout(() => {
          if (canReconnect()) {
            setConnectionStatus('CONNECTING');
            connectToWebSocket();
          }
        }, 2000);
      }
    });

    return () => unsubscribe();
  }, [networkConnected, ws, appState, canReconnect, connectToWebSocket, showConnectionNotification]);

  // 컴포넌트 언마운트 시 정리
  useEffect(() => {
    return () => {
      console.log('📱 MatchChatRoomScreen 언마운트됨');
      disconnect();
    };
  }, [disconnect]);

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <KeyboardAvoidingView 
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 0}
      >
        {/* 알림 관리자 */}
        <ChatNotificationManager
          notifications={notifications}
          onDismiss={dismissNotification}
        />

        {/* 헤더 */}
        <View style={[styles.header, { backgroundColor: themeColor }]}>
          <TouchableOpacity
            onPress={() => navigation.goBack()}
            style={styles.backButton}
          >
            <Text style={styles.backButtonText}>←</Text>
          </TouchableOpacity>
          <View style={styles.headerContent}>
            <View style={styles.headerTitleRow}>
              <Text style={styles.headerTitle}>
                {isWatchChat ? '직관채팅' : room.matchTitle || '매치채팅'}
              </Text>
              <SimpleConnectionStatus status={connectionStatus} />
            </View>
            <ConnectionStatusIndicator 
              status={connectionStatus}
              reconnectAttempts={connectionStateRef.current.reconnectAttempts}
              maxReconnectAttempts={connectionStateRef.current.maxReconnectAttempts}
            />
          </View>
        </View>

        {/* 메시지 목록 */}
        <ScrollView
          ref={scrollViewRef}
          style={styles.messagesList}
          contentContainerStyle={styles.messagesContent}
        >
          {/* 실제 메시지들 */}
          {messages.map((message, index) => (
            <View key={message.id || index} style={styles.messageItem}>
              {message.messageType === 'CHAT' ? (
                <View style={[
                  styles.chatMessage,
                  (message as any)._isMyMessage && styles.myMessage
                ]}>
                  <View style={styles.messageHeader}>
                    <Text style={styles.messageNickname}>{(message as MatchChatMessage).nickname}</Text>
                    {(message as any)._isMyMessage && (
                      <SimpleMessageStatus 
                        status={message.status} 
                        size={14}
                      />
                    )}
                  </View>
                  <Text style={styles.messageContent}>{message.content}</Text>
                  <Text style={styles.messageTime}>
                    {new Date(message.timestamp).toLocaleTimeString('ko-KR', { 
                      hour: '2-digit', 
                      minute: '2-digit' 
                    })}
                  </Text>
                  
                  {/* 내 메시지의 상태 표시 */}
                  {(message as any)._isMyMessage && message.status && message.status !== 'sent' && (
                    <MessageStatusIndicator
                      status={message.status}
                      onRetry={message.id ? () => retryMessage(message.id!) : undefined}
                      retryCount={message.retryCount}
                      maxRetries={3}
                    />
                  )}
                </View>
              ) : (
                <View style={styles.systemMessage}>
                  <Text style={styles.systemMessageText}>{message.content}</Text>
                </View>
              )}
            </View>
          ))}
          
          {/* 대기 중인 메시지들 표시 */}
          {pendingMessages.map((pendingMsg) => (
            <View key={pendingMsg.id} style={styles.messageItem}>
              <View style={[styles.chatMessage, styles.myMessage, styles.pendingMessage]}>
                <View style={styles.messageHeader}>
                  <Text style={styles.messageNickname}>
                    {currentUser?.nickname || 'Anonymous'}
                  </Text>
                  <SimpleMessageStatus 
                    status={pendingMsg.status} 
                    size={14}
                  />
                </View>
                <Text style={styles.messageContent}>{pendingMsg.content}</Text>
                <Text style={styles.messageTime}>
                  {new Date(pendingMsg.timestamp).toLocaleTimeString('ko-KR', { 
                    hour: '2-digit', 
                    minute: '2-digit' 
                  })}
                </Text>
                
                <MessageStatusIndicator
                  status={pendingMsg.status}
                  onRetry={() => retryMessage(pendingMsg.id)}
                  retryCount={pendingMsg.retryCount}
                  maxRetries={pendingMsg.maxRetries}
                />
              </View>
            </View>
          ))}
        </ScrollView>

        {/* 메시지 입력 */}
        <View style={styles.messageInput}>
          <TextInput
            style={styles.textInput}
            value={currentMessage}
            onChangeText={setCurrentMessage}
            placeholder="메시지를 입력하세요..."
            multiline
            maxLength={500}
          />
          <TouchableOpacity
            style={[
              styles.sendButton,
              { backgroundColor: themeColor },
              !currentMessage.trim() && styles.sendButtonDisabled
            ]}
            onPress={sendMessage}
            disabled={!currentMessage.trim() || connectionStatus !== 'CONNECTED'}
          >
            <Text style={styles.sendButtonText}>전송</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
};