import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  FlatList,
  Alert,
  TextInput,
  KeyboardAvoidingView,
  Platform,
  AppState,
  Image,
  ActivityIndicator,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
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
import { gameApi } from '../../entities/game/api/api';
import { chatRoomApi } from '../../entities/chat-room/api/api';

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
  const [inputKey, setInputKey] = useState(0); // TextInput 강제 리렌더링용
  const getCurrentUser = useUserStore((state) => state.getCurrentUser);
  const currentUser = getCurrentUser();
  const currentUserId = currentUser?.userId || 45;
  
  const flatListRef = useRef<FlatList>(null);
  const textInputRef = useRef<TextInput>(null);

  // 메시지 고유 ID 생성 함수
  const generateMessageId = useCallback((message: any, index?: number) => {
    if (message.id) return message.id;
    
    const timestamp = message.timestamp ? new Date(message.timestamp).getTime() : Date.now();
    const userId = message.userId || 'unknown';
    const content = message.content ? message.content.substring(0, 10) : 'no-content';
    const indexSuffix = index !== undefined ? `_${index}` : '';
    
    return `msg_${timestamp}_${userId}_${content}${indexSuffix}`;
  }, []);

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

  // messages 상태 변경 추적
  React.useEffect(() => {
    console.log('🎯 📊 Messages 상태 업데이트:', {
      count: messages.length,
      firstMessage: messages[0]?.timestamp,
      lastMessage: messages[messages.length - 1]?.timestamp,
      sampleMessages: messages.slice(0, 5).map(m => ({
        timestamp: m.timestamp,
        content: m.content?.substring(0, 20)
      }))
    });
  }, [messages]);
  const [ws, setWs] = useState<WebSocket | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ExtendedConnectionStatus>('DISCONNECTED');
  const [sentMessages, setSentMessages] = useState<Set<string>>(new Set());
  const [appState, setAppState] = useState(AppState.currentState);
  const [networkConnected, setNetworkConnected] = useState(true);
  const [isInitialLoad, setIsInitialLoad] = useState(true);
  const [gameInfo, setGameInfo] = useState<any>(null);
  
  // 추가 메시지 로딩 관련 상태
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasMoreMessages, setHasMoreMessages] = useState(true);
  const [oldestMessageTimestamp, setOldestMessageTimestamp] = useState<number | null>(null);
  
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

  const scrollToBottom = useCallback((animated: boolean = false) => {
    if (messages.length === 0) return;
    
    if (animated) {
      setTimeout(() => {
        flatListRef.current?.scrollToEnd({ animated: true });
      }, 50);
    } else {
      // 애니메이션 없이 즉시 스크롤
      flatListRef.current?.scrollToEnd({ animated: false });
    }
  }, [messages.length]);


  const addMessage = useCallback((message: ChatMessage, isMyMessage: boolean = false) => {
    setMessages(prev => {
      const isDuplicate = prev.some(m => 
        m.timestamp === message.timestamp && 
        m.content === message.content &&
        m.userId === message.userId
      );
      
      if (isDuplicate) return prev;
      
      const markedMessage = {
        ...message,
        _isMyMessage: isMyMessage
      };
      
      const newMessages = [...prev, markedMessage];
      newMessages.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
      
      return newMessages;
    });
  }, []);

  // 추가 메시지 로드 함수
  const loadMoreMessages = useCallback(async () => {
    if (isLoadingMore || !hasMoreMessages || !oldestMessageTimestamp) {
      return;
    }

    setIsLoadingMore(true);
    
    try {
      // API 호출로 이전 메시지 조회
      const response = await chatRoomApi.getMatchChatHistory({
        matchId: room.matchId,
        lastMessageTimestamp: oldestMessageTimestamp,
        limit: 100
      });
      
      if (response.status === 'SUCCESS' && response.data.messages) {
        const moreMessages = response.data.messages;
        
        console.log('📚 API로 조회된 추가 메시지 수:', moreMessages.length);
        
        const processedMessages = moreMessages.map((msg: any) => ({
          ...msg,
          timestamp: typeof msg.timestamp === 'number' ? new Date(msg.timestamp).toISOString() : msg.timestamp,
          _isMyMessage: msg.userId && (
            msg.userId.toString() === currentUserId.toString() ||
            msg.userId.toString() === currentUser?.userId?.toString()
          )
        }));
        
        addMessagesFromHistory(processedMessages);
        
        // 서버에서 hasMore 정보가 없으면 메시지 개수로 판단
        if (!response.data.hasMore || moreMessages.length < 100) {
          setHasMoreMessages(false);
        }
      } else {
        setHasMoreMessages(false);
      }
      
    } catch (error) {
      console.error('추가 메시지 로드 실패:', error);
      setHasMoreMessages(false);
    } finally {
      setIsLoadingMore(false);
    }
  }, [isLoadingMore, hasMoreMessages, oldestMessageTimestamp, room.matchId, currentUserId, currentUser, addMessagesFromHistory]);

  // 메시지 목록에 새 메시지들 추가 (중복 제거 및 타임스탬프 업데이트)
  const addMessagesFromHistory = useCallback((newMessages: ChatMessage[]) => {
    if (newMessages.length === 0) {
      setHasMoreMessages(false);
      setIsLoadingMore(false);
      return;
    }

    setMessages(prev => {
      // 기존 메시지들의 고유 키를 생성
      const existingKeys = new Set(prev.map(m => `${m.timestamp}_${m.content}_${m.userId}`));
      const uniqueNewMessages = newMessages.filter(msg => {
        const msgKey = `${msg.timestamp}_${msg.content}_${msg.userId}`;
        return !existingKeys.has(msgKey);
      });
      
      if (uniqueNewMessages.length === 0) {
        setHasMoreMessages(false);
        setIsLoadingMore(false);
        return prev;
      }

      const combined = [...prev, ...uniqueNewMessages];
      combined.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
      
      // 가장 오래된 메시지의 타임스탬프 업데이트
      if (combined.length > 0) {
        const oldestTimestamp = new Date(combined[0].timestamp).getTime();
        setOldestMessageTimestamp(oldestTimestamp);
      }

      return combined;
    });

    // 새로 로드된 메시지가 100개 미만이면 더 이상 없다고 판단
    if (newMessages.length < 100) {
      setHasMoreMessages(false);
    }

    setIsLoadingMore(false);
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
        
        // 초기 히스토리 로딩 완료 타이머 (3초 후 초기 로딩 완료로 간주)
        setTimeout(() => {
          setIsInitialLoad(false);
          console.log('🏁 초기 로딩 완료, 일반 모드로 전환');
        }, 3000);
        
        // 사용자에게 연결 성공 알림
        showConnectionNotification('CONNECTED');
        
        // 대기 중인 메시지들 재전송 시도
        // await flushQueue();
        
        // 인증 데이터 전송 (기존 로직 유지)
        let authData;
        if (isWatchChat) {
          if (!currentUser?.teamId) {
            console.warn('⚠️ 사용자 teamId가 없습니다. 기본값(두산) 사용');
          }
          authData = {
            type: 'AUTH',
            gameId: room.gameId || '1303',
            teamId: currentUser?.teamId || 9, // 두산 베어스 기본값
            nickname: currentUser?.nickname || 'Anonymous',
            userId: currentUser?.userId || currentUserId
          };
        } else {
          authData = {
            type: 'AUTH',
            matchId: room.matchId,
            nickname: currentUser?.nickname || 'Anonymous',
            winRate: currentUser?.winRate || 0,
            profileImgUrl: currentUser?.profileImg || '',
            isWinFairy: (currentUser?.winRate || 0) >= 70
          };
        }
        
        websocket.send(JSON.stringify(authData));
      };

      websocket.onmessage = (event) => {
        // 메시지 처리 로직 (기존과 동일)
        try {
          const messageData = JSON.parse(event.data);
          
          // 히스토리 응답 처리 - 서버에서 받은 모든 메시지 표시
          if (messageData.type === 'HISTORY_RESPONSE' || messageData.type === 'INITIAL_HISTORY') {
            console.log('📥 히스토리 응답 수신:', {
              type: messageData.type,
              messagesCount: messageData.messages?.length || 0
            });
            
            const historyMessages = messageData.messages || [];
            
            if (historyMessages.length > 0) {
              console.log('📚 🔍 히스토리 원본 데이터 분석:', {
                totalMessages: historyMessages.length,
                firstMessage: historyMessages[0],
                lastMessage: historyMessages[historyMessages.length - 1],
                sampleMessages: historyMessages.slice(0, 3).map(m => ({
                  timestamp: m.timestamp,
                  userId: m.userId,
                  content: m.content?.substring(0, 30)
                }))
              });
              
              // 모든 메시지를 한번에 설정
              const processedMessages = historyMessages.map((msg: any, index: number) => ({
                ...msg,
                timestamp: typeof msg.timestamp === 'number' ? new Date(msg.timestamp).toISOString() : msg.timestamp,
                _isMyMessage: msg.userId && (
                  msg.userId.toString() === currentUserId.toString() ||
                  msg.userId.toString() === currentUser?.userId?.toString()
                )
              }));
              
              console.log('📚 🔄 메시지 전처리 완료:', {
                processedCount: processedMessages.length,
                sampleProcessed: processedMessages.slice(0, 3).map(m => ({
                  timestamp: m.timestamp,
                  userId: m.userId,
                  content: m.content?.substring(0, 30),
                  _isMyMessage: m._isMyMessage
                }))
              });
              
              // timestamp로 정렬
              processedMessages.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
              
              console.log('📚 📅 정렬 후 메시지:', {
                sortedCount: processedMessages.length,
                oldestMessage: processedMessages[0]?.timestamp,
                newestMessage: processedMessages[processedMessages.length - 1]?.timestamp
              });
              
              setMessages(processedMessages);
              
              // 가장 오래된 메시지의 타임스탬프 설정
              if (processedMessages.length > 0) {
                const oldestTimestamp = new Date(processedMessages[0].timestamp).getTime();
                setOldestMessageTimestamp(oldestTimestamp);
              }
              
              console.log('✅ 🎯 전체 메시지 로드 최종 완료:', processedMessages.length, '개');
            }
            return;
          }

          
          if (typeof messageData.timestamp === 'number') {
            messageData.timestamp = new Date(messageData.timestamp).toISOString();
          }
          
          const messageKey = `${messageData.content}_${messageData.timestamp}`;
          const isMyMessage = messageData.userId && (
            messageData.userId.toString() === currentUserId.toString() ||
            messageData.userId.toString() === currentUser?.userId?.toString()
          ) || (!isWatchChat && messageData.nickname === currentUser?.nickname);
          
          
          
          if (messageData.messageType === 'CHAT' || messageData.type === 'CHAT_MESSAGE') {
            const content = messageData.content || '';
            
            const isAuthDataMessage = typeof content === 'string' && (
              content.includes('"matchId"') && content.includes('"winRate"') ||
              content.includes('"gameId"') && content.includes('"teamId"') ||
              content.includes('"isWinFairy"') ||
              content.includes('"profileImgUrl"')
            );
              
            if (!isAuthDataMessage) {
              if (isWatchChat) {
                // 직관채팅: 단순하게 메시지 추가
                addMessage(messageData, isMyMessage);
              } else {
                // 매치채팅: 내 메시지인 경우 로컬 메시지를 서버 메시지로 교체
                if (isMyMessage) {
                  setMessages(prev => {
                    // 같은 내용의 로컬 메시지 제거 (pending 메시지)
                    const filtered = prev.filter(m => 
                      !(m.content === messageData.content && (m as any)._isMyMessage && m.status)
                    );
                    
                    // 서버 메시지 추가
                    const serverMessage = {
                      ...messageData,
                      _isMyMessage: true
                    };
                    
                    const newMessages = [...filtered, serverMessage];
                    newMessages.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
                    return newMessages;
                  });
                } else {
                  // 다른 사용자 메시지는 그대로 추가
                  console.log('🔄 개별 메시지 추가 시도:', {
                    timestamp: messageData.timestamp,
                    content: messageData.content?.substring(0, 30),
                    currentMessageCount: messages.length,
                    isInitialLoad: isInitialLoad
                  });
                  addMessage(messageData, isMyMessage);
                }
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
    const state = connectionStateRef.current;
    
    // 이미 해제된 경우 중복 실행 방지
    if (state.isDestroyed) {
      console.log('📱 WebSocket 이미 해제됨 - 중복 호출 무시');
      return;
    }
    
    console.log('📱 WebSocket 연결 해제');
    
    state.isDestroyed = true;
    state.isConnecting = false;
    state.isConnected = false;
    
    clearReconnectTimer();
    
    if (ws && ws.readyState === WebSocket.OPEN) {
      try {
        ws.close(1000, 'Client disconnect');
      } catch (error) {
        console.log('📱 WebSocket 종료 중 에러 (무시):', error);
      }
    }
    setWs(null);
    
    setConnectionStatus('DISCONNECTED');
    setSentMessages(new Set());
  }, [ws, clearReconnectTimer]);

  const sendMessage = useCallback(async () => {
    if (!currentMessage.trim()) return;

    const messageContent = currentMessage.trim();
    
    // 강력한 입력 필드 초기화
    setCurrentMessage('');
    setInputKey(prev => prev + 1); // TextInput 강제 리마운트
    
    // 추가적인 초기화 (비동기)
    setTimeout(() => {
      textInputRef.current?.clear();
      setCurrentMessage('');
    }, 0);
    
    try {
      // 메시지 큐에 추가 (자동으로 전송 시도)
      const messageId = await addMessageToQueue(messageContent);
      
      // 매치채팅에서만 로컬에서 즉시 메시지 표시 (낙관적 업데이트)
      if (!isWatchChat) {
        const timestamp = new Date().toISOString();
        const localMessage: MessageWithStatus = {
          messageType: 'CHAT',
          roomId: room.matchId || '',
          userId: currentUser?.userId?.toString() || currentUserId.toString(),
          nickname: currentUser?.nickname || 'Anonymous',
          content: messageContent,
          timestamp,
          id: messageId,
          status: 'sent',
          _isMyMessage: true,
        };
        
        addMessage(localMessage, true);
      }
      
      console.log('메시지 큐에 추가:', messageContent);
    } catch (error) {
      console.error('메시지 전송 오류:', error);
      const chatError = getErrorMessage({
        type: 'MESSAGE_SEND',
        message: error instanceof Error ? error.message : '메시지 전송 실패'
      });
      logChatError(chatError, { content: messageContent });
      showErrorNotification(chatError);
      
      // 에러 발생 시에도 입력창은 이미 클리어됨 (사용자가 새 메시지 입력 가능)
    }
  }, [currentMessage, addMessageToQueue, addMessage, room, currentUser, currentUserId, showErrorNotification]);

  // 🔧 FIX 7: useEffect 의존성 배열 최적화
  
  // inverted FlatList 사용으로 자동 스크롤 처리됨

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

    return () => {
      unsubscribeBeforeRemove();
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

  // 게임 정보 로드
  useEffect(() => {
    const loadGameInfo = async () => {
      if (room.gameId) {
        try {
          const response = await gameApi.getGameById(room.gameId.toString());
          if (response.status === 'SUCCESS') {
            setGameInfo(response.data);
          }
        } catch (error) {
          console.error('게임 정보 로드 실패:', error);
        }
      }
    };
    
    loadGameInfo();
  }, [room.gameId]);

  // 컴포넌트 언마운트 시 정리
  useEffect(() => {
    return () => {
      console.log('📱 MatchChatRoomScreen 언마운트됨');
      disconnect();
    };
  }, []); // disconnect를 dependency에서 제거

  return (
    <View style={styles.container}>
      <LinearGradient
        colors={[themeColor, themeColor]}
        style={[styles.headerGradient, { paddingTop: insets.top }]}
      >
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => navigation.goBack()}
            style={styles.backButton}
          >
            <Text style={[styles.backButtonText, { color: '#ffffff' }]}>←</Text>
          </TouchableOpacity>
          <View style={styles.headerContent}>
            <Text style={[styles.headerTitle, { color: '#ffffff' }]}>
              {isWatchChat ? '직관채팅' : room.matchTitle || '매치채팅'}
            </Text>
            {gameInfo && (
              <Text style={[styles.headerSubtitle, { color: '#ffffff' }]}>
                {gameInfo.awayTeamName} vs {gameInfo.homeTeamName}
              </Text>
            )}
          </View>
        </View>
      </LinearGradient>

      <KeyboardAvoidingView 
        style={styles.keyboardContainer}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? insets.top + 56 : 0}
      >
        {/* 알림 관리자 */}
        <ChatNotificationManager
          notifications={notifications}
          onDismiss={dismissNotification}
        />

        {/* 메시지 목록 */}
        <FlatList
          ref={flatListRef}
          style={styles.messagesList}
          contentContainerStyle={styles.messagesContent}
          data={[...messages, ...pendingMessages.map(p => ({ ...p, _isPending: true }))].reverse()}
          keyExtractor={(item, index) => {
            const baseKey = generateMessageId(item, index);
            
            // pending 메시지는 별도 prefix 추가
            return (item as any)._isPending ? `pending_${baseKey}` : baseKey;
          }}
          onEndReached={loadMoreMessages}
          onEndReachedThreshold={0.5}
          ListFooterComponent={
            isLoadingMore ? (
              <View style={{ paddingVertical: 20, alignItems: 'center' }}>
                <ActivityIndicator size="small" color="#007AFF" />
                <Text style={{ marginTop: 8, color: '#666', fontSize: 12 }}>
                  이전 메시지를 불러오는 중...
                </Text>
              </View>
            ) : null
          }
          renderItem={({ item }) => (
            <View style={styles.messageItem}>
              {(item.messageType === 'CHAT' || item.type === 'CHAT_MESSAGE') ? (
                <View style={[
                  styles.messageRow,
                  ((item as any)._isMyMessage || (item as any)._isPending) ? styles.myMessageRow : styles.otherMessageRow
                ]}>
                  {/* 프로필 사진 (내 메시지가 아닌 경우만) */}
                  {!((item as any)._isMyMessage || (item as any)._isPending) && (
                    <View style={styles.profileImageContainer}>
                      {(() => {
                        const message = item as MatchChatMessage;
                        
                        // 더미 URL이거나 유효하지 않은 URL인 경우 기본 아바타 표시
                        const isValidUrl = message.profileImgUrl && 
                                         !message.profileImgUrl.includes('example.com') && 
                                         message.profileImgUrl.startsWith('http');
                        
                        if (isValidUrl) {
                          return (
                            <Image 
                              source={{ uri: message.profileImgUrl }}
                              style={styles.profileImage}
                              onError={() => {}}
                              onLoad={() => {}}
                            />
                          );
                        } else {
                          // 더미 URL 또는 유효하지 않은 URL, 기본 아바타 사용
                          return (
                            <View style={[styles.profileImage, { backgroundColor: '#E0E0E0', justifyContent: 'center', alignItems: 'center' }]}>
                              <Text style={{ fontSize: 20, color: '#666' }}>👤</Text>
                            </View>
                          );
                        }
                      })()}
                      {/* 승리요정인 경우 왕관 아이콘 */}
                      {(item as MatchChatMessage).isWinFairy && (
                        <View style={styles.crownIcon}>
                          <Text style={styles.crownEmoji}>👑</Text>
                        </View>
                      )}
                    </View>
                  )}
                  
                  {/* 메시지 영역 */}
                  <View style={[
                    styles.messageBubbleContainer,
                    ((item as any)._isMyMessage || (item as any)._isPending) ? styles.myMessageBubbleContainer : styles.otherMessageBubbleContainer
                  ]}>
                    {/* 닉네임 (내 메시지가 아닌 경우만) */}
                    {!((item as any)._isMyMessage || (item as any)._isPending) && (
                      <Text style={styles.messageNickname}>
                        {(item as MatchChatMessage).nickname}
                      </Text>
                    )}
                    
                    {/* 말풍선과 시간을 담는 컨테이너 */}
                    <View style={[
                      styles.bubbleAndTimeContainer,
                      ((item as any)._isMyMessage || (item as any)._isPending) ? styles.myBubbleAndTimeContainer : styles.otherBubbleAndTimeContainer
                    ]}>
                      {/* 시간 (내 메시지인 경우 왼쪽에) */}
                      {((item as any)._isMyMessage || (item as any)._isPending) && (
                        <Text style={styles.myMessageTime}>
                          {new Date(item.timestamp).toLocaleTimeString('ko-KR', { 
                            hour: '2-digit', 
                            minute: '2-digit' 
                          })}
                        </Text>
                      )}
                      
                      {/* 말풍선 */}
                      <View style={[
                        styles.chatMessage,
                        ((item as any)._isMyMessage || (item as any)._isPending) ? styles.myMessage : styles.otherMessage
                      ]}>
                        <Text style={[
                          ((item as any)._isMyMessage || (item as any)._isPending) ? styles.myMessageContent : styles.messageContent
                        ]}>
                          {item.content}
                        </Text>
                      </View>
                      
                      {/* 시간 (다른 사용자 메시지인 경우 오른쪽에) */}
                      {!((item as any)._isMyMessage || (item as any)._isPending) && (
                        <Text style={styles.otherMessageTime}>
                          {new Date(item.timestamp).toLocaleTimeString('ko-KR', { 
                            hour: '2-digit', 
                            minute: '2-digit' 
                          })}
                        </Text>
                      )}
                    </View>
                  </View>
                </View>
              ) : (
                <View style={styles.systemMessage}>
                  <Text style={styles.systemMessageText}>{item.content}</Text>
                </View>
              )}
            </View>
          )}
          inverted
          showsVerticalScrollIndicator={false}
        />

        {/* 메시지 입력 */}
        <View style={[
          styles.messageInputWithSafeArea, 
          { paddingBottom: Math.max(insets.bottom, 16) }
        ]}>
          <TextInput
            key={inputKey}
            ref={textInputRef}
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