import React, { useState, useEffect, useRef } from 'react';
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
import { useMatchChatWebSocket } from '../../features/match-chat';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { ChatMessage, MatchChatMessage, SystemMessage } from '../../features/match-chat';
import type { ChatStackParamList } from '../../navigation/types';
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
  const currentUserId = currentUser?.userId || 45; // fallback to test ID that matches log
  
  
  const scrollViewRef = useRef<ScrollView>(null);

  const scrollToBottom = () => {
    setTimeout(() => {
      scrollViewRef.current?.scrollToEnd({ animated: true });
    }, 50);
  };

  // 기존 WebSocket 로직 다시 사용
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [ws, setWs] = useState<WebSocket | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR'>('DISCONNECTED');
  const [sentMessages, setSentMessages] = useState<Set<string>>(new Set());
  const [appState, setAppState] = useState(AppState.currentState);
  const [isReconnecting, setIsReconnecting] = useState(false);
  const [reconnectAttempts, setReconnectAttempts] = useState(0);
  const [networkConnected, setNetworkConnected] = useState(true);
  const [componentId] = useState(() => Math.random().toString(36).substr(2, 9)); // 컴포넌트 고유 ID
  const maxReconnectAttempts = 3;

  const addMessage = (message: ChatMessage, isMyMessage: boolean = false) => {
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
  };

  // 메시지가 업데이트될 때마다 스크롤을 하단으로
  useEffect(() => {
    if (messages.length > 0) {
      scrollToBottom();
    }
  }, [messages]);

  const connectToWebSocket = async () => {
    try {
      // 네트워크 상태 먼저 확인
      const netState = await NetInfo.fetch();
      if (!netState.isConnected) {
        console.log('🌐 네트워크 연결 없음 - WebSocket 연결 시도 중단');
        setConnectionStatus('ERROR');
        setIsReconnecting(false);
        return;
      }

      // 이미 연결 중이거나 연결되어 있으면 중복 연결 방지
      if (ws && (ws.readyState === WebSocket.CONNECTING || ws.readyState === WebSocket.OPEN)) {
        console.log(`📱 이미 WebSocket 연결 중 또는 연결됨 - 중복 연결 방지 [${componentId}]`, ws.readyState);
        return;
      }
      
      // 재연결 중이면 방지
      if (isReconnecting) {
        console.log(`📱 이미 재연결 진행 중 - 중복 연결 방지 [${componentId}]`);
        return;
      }
      
      console.log(`📱 WebSocket 연결 시도 시작 [${componentId}]`, { websocketUrl });
      setConnectionStatus('CONNECTING');
      setIsReconnecting(true);
      
      // 안드로이드 에뮬레이터용 URL 변경
      let wsUrl = websocketUrl;
      if (wsUrl && wsUrl.includes('localhost')) {
        wsUrl = wsUrl.replace('localhost', '10.0.2.2');
      }
      
      // WebSocket URL 검증 및 수정
      if (wsUrl && !wsUrl.startsWith('ws://') && !wsUrl.startsWith('wss://')) {
        if (wsUrl.startsWith('http://')) {
          wsUrl = wsUrl.replace('http://', 'ws://');
        } else if (wsUrl.startsWith('https://')) {
          wsUrl = wsUrl.replace('https://', 'wss://');
        }
      }
      
      // 서버 연결 문제로 인한 임시 우회: 데모용 WebSocket 연결 시뮬레이션
      // mock 토큰이 있는 경우에만 데모 모드 사용
      if (wsUrl && wsUrl.includes('i13a403.p.ssafy.io:8083') && sessionToken && sessionToken.startsWith('mock_session_token')) {
        console.log('⚠️ 서버 WebSocket 연결 문제로 인한 임시 데모 모드 (목 토큰 감지)');
        // 연결 성공으로 시뮬레이션
        setTimeout(() => {
          setConnectionStatus('CONNECTED');
          console.log('📡 데모 모드: 연결 성공으로 시뮬레이션됨');
          
          // 샘플 메시지 추가
          const welcomeMessage = {
            messageType: 'SYSTEM' as const,
            content: '채팅방에 연결되었습니다. (데모 모드)',
            timestamp: new Date().toISOString(),
            userId: 'system',
            nickname: 'System'
          };
          addMessage(welcomeMessage, false);
        }, 1000);
        
        return;
      }
      
      console.log('웹소켓 연결 시작');
      console.log('원본 websocketUrl:', websocketUrl);
      console.log('sessionToken:', sessionToken);
      console.log(`최종 wsUrl: ${wsUrl}`);

      if (!wsUrl) {
        console.error('웹소켓 URL이 없습니다!');
        setConnectionStatus('DISCONNECTED');
        return;
      }

      // WebSocket 연결 (React Native에서는 옵션 객체를 지원하지 않음)
      const websocket = new WebSocket(wsUrl);
      setWs(websocket);

      websocket.onopen = () => {
        setConnectionStatus('CONNECTED');
        setReconnectAttempts(0); // 연결 성공 시 재연결 시도 횟수 리셋
        setIsReconnecting(false); // 재연결 상태 해제
        console.log('📡 WebSocket 연결 성공');
        
        // 매치채팅과 직관채팅 모두 사용자 정보 전송
        const isWatchChat = wsUrl.includes('/ws/watch-chat/') || (wsUrl.includes('gameId=') && wsUrl.includes('teamId='));
        
        let authData;
        if (isWatchChat) {
          // 직관채팅용 인증 데이터
          authData = {
            type: 'AUTH',
            gameId: room.gameId || '1258',
            teamId: currentUser?.teamId || 3,
            nickname: currentUser?.nickname || 'Anonymous',
            userId: currentUser?.userId || currentUserId
          };
        } else {
          // 매치채팅용 인증 데이터
          authData = {
            type: 'AUTH',
            matchId: room.matchId,
            nickname: currentUser?.nickname || 'Anonymous',
            winRate: 75,
            profileImgUrl: currentUser?.profileImageURL || '',
            isWinFairy: false
          };
        }
        
        console.log('🔐 WebSocket 인증 데이터 전송 (', isWatchChat ? '직관채팅' : '매치채팅', '):', JSON.stringify(authData, null, 2));
        websocket.send(JSON.stringify(authData));
      };

      websocket.onmessage = (event) => {
        try {
          const messageData = JSON.parse(event.data);
          console.log('메시지 수신:', messageData);
          
          // timestamp 형식 통일 (숫자인 경우 ISO 문자열로 변환)
          if (typeof messageData.timestamp === 'number') {
            messageData.timestamp = new Date(messageData.timestamp).toISOString();
          }
          
          const messageKey = `${messageData.content}_${messageData.timestamp}`;
          const isMyMessage = sentMessages.has(messageKey);
          
          // 매치채팅: messageType === 'CHAT', 직관채팅: type === 'CHAT_MESSAGE'
          if (messageData.messageType === 'CHAT' || messageData.type === 'CHAT_MESSAGE') {
            const content = messageData.content || '';
            
            // 인증 데이터가 메시지 content로 전송된 경우 입장 메시지로 변환
            const isAuthDataMessage = typeof content === 'string' && (
              content.includes('"matchId"') && content.includes('"winRate"') ||
              content.includes('"gameId"') && content.includes('"teamId"') ||
              content.includes('"isWinFairy"') ||
              content.includes('"profileImgUrl"')
            );
              
            if (isAuthDataMessage) {
              // 직관채팅은 익명이므로 입장 메시지를 표시하지 않음
              if (isWatchChat) {
                console.log('🚫 직관채팅 인증 데이터 필터링됨 (익명)');
              } else {
                // 매치채팅만 입장 메시지 생성
                try {
                  const authData = JSON.parse(content);
                  const nickname = authData.nickname || messageData.nickname || '사용자';
                  
                  const joinMessage = {
                    messageType: 'USER_JOIN' as const,
                    content: `${nickname}님이 입장했습니다.`,
                    timestamp: messageData.timestamp,
                    userId: messageData.userId || 'system',
                    nickname: 'System',
                    userName: nickname
                  };
                  
                  addMessage(joinMessage, false);
                  console.log('✅ 매치채팅 입장 메시지로 변환:', nickname);
                } catch (e) {
                  console.error('인증 데이터 파싱 실패:', e);
                }
              }
            } else if (content.trim()) {
              addMessage(messageData, isMyMessage);
              console.log('✅ 정상 메시지 추가:', content);
            }
            
            if (isMyMessage) {
              setSentMessages(prev => {
                const newSet = new Set(prev);
                newSet.delete(messageKey);
                return newSet;
              });
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
        setConnectionStatus('DISCONNECTED');
        console.log(`웹소켓 연결 종료: ${event.code} - ${event.reason}`);
        
        // 정상 종료(1000)가 아니고 네트워크 연결되어있고 재연결 한도 내에서만 재시도
        if (event.code !== 1000 && !isReconnecting && appState === 'active' && networkConnected && reconnectAttempts < maxReconnectAttempts) {
          setIsReconnecting(true);
          setReconnectAttempts(prev => prev + 1);
          
          // 지수적 백오프: 2^n * 1000ms (최대 10초)
          const backoffDelay = Math.min(Math.pow(2, reconnectAttempts) * 1000, 10000);
          console.log(`🔄 재연결 시도 ${reconnectAttempts + 1}/${maxReconnectAttempts} (${backoffDelay}ms 후)`);
          
          setTimeout(() => {
            if (appState === 'active' && reconnectAttempts < maxReconnectAttempts) {
              connectToWebSocket();
            }
            setIsReconnecting(false);
          }, backoffDelay);
        } else if (reconnectAttempts >= maxReconnectAttempts) {
          console.log('❌ 최대 재연결 시도 횟수 초과. 연결을 포기합니다.');
          setConnectionStatus('ERROR');
        }
      };

      websocket.onerror = (error) => {
        setConnectionStatus('ERROR');
        console.error('웹소켓 오류:', error);
        console.log('웹소켓 오류 상세:', JSON.stringify(error, null, 2));
        
        // 연결 타임아웃 오류인지 확인
        const errorMessage = error.message || '';
        const isConnectionTimeout = errorMessage.includes('failed to connect') || errorMessage.includes('timeout');
        
        // 연결 타임아웃 오류면 재연결 시도 안 함
        if (isConnectionTimeout) {
          console.log('❌ 서버 연결 실패 - 재연결을 시도하지 않습니다.');
          setConnectionStatus('ERROR');
          setIsReconnecting(false); // 재연결 상태 해제
          return;
        }
        
        // 재연결 중이 아니고 앱이 활성 상태이며 네트워크 연결되어있고 재연결 한도 내에서만 재시도
        if (!isReconnecting && appState === 'active' && networkConnected && reconnectAttempts < maxReconnectAttempts) {
          setIsReconnecting(true);
          setReconnectAttempts(prev => prev + 1);
          
          // 지수적 백오프: 2^n * 1000ms (최대 10초)
          const backoffDelay = Math.min(Math.pow(2, reconnectAttempts) * 1000, 10000);
          console.log(`🔄 에러로 인한 재연결 시도 ${reconnectAttempts + 1}/${maxReconnectAttempts} (${backoffDelay}ms 후)`);
          
          setTimeout(() => {
            if (appState === 'active' && reconnectAttempts < maxReconnectAttempts) {
              connectToWebSocket();
            }
            setIsReconnecting(false);
          }, backoffDelay);
        } else if (reconnectAttempts >= maxReconnectAttempts) {
          console.log('❌ 최대 재연결 시도 횟수 초과. 연결을 포기합니다.');
          setConnectionStatus('ERROR');
        }
      };

    } catch (error) {
      console.error('📱 WebSocket 연결 시도 중 오류:', error);
      setConnectionStatus('ERROR');
      setIsReconnecting(false); // 재연결 상태 해제
    }
  };

  const disconnect = () => {
    console.log(`📱 WebSocket disconnect 호출됨 [${componentId}]`);
    if (ws) {
      console.log(`📱 기존 WebSocket 연결 정리 중... [${componentId}]`, ws.readyState);
      ws.close(1000, 'User disconnected');
      setWs(null);
      setConnectionStatus('DISCONNECTED');
      setSentMessages(new Set());
      setIsReconnecting(false);
      setReconnectAttempts(0);
      console.log(`📱 WebSocket 연결 해제 완료 [${componentId}]`);
    }
  };

  const sendMessage = async () => {
    if (!currentMessage.trim()) {
      Alert.alert('알림', '메시지를 입력해주세요.');
      return;
    }

    // 네트워크 상태 확인
    const netState = await NetInfo.fetch();
    if (!netState.isConnected) {
      Alert.alert('네트워크 오류', '네트워크 연결을 확인해주세요.');
      return;
    }

    if (ws && ws.readyState === WebSocket.OPEN) {
      try {
        const messageContent = currentMessage.trim();
        const timestamp = new Date().toISOString();
        
        const messageKey = `${messageContent}_${timestamp}`;
        setSentMessages(prev => new Set([...prev, messageKey]));
        
        ws.send(messageContent);
        setCurrentMessage('');
        console.log('메시지 전송:', messageContent);
        
        // 메시지 전송 후 스크롤
        scrollToBottom();
      } catch (error) {
        console.error('메시지 전송 오류:', error);
        Alert.alert('전송 오류', '메시지 전송에 실패했습니다.');
      }
    } else {
      Alert.alert('오류', '채팅방에 연결되지 않았습니다.');
    }
  };

  const formatTime = (timestamp: string): string => {
    const date = new Date(timestamp);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  };

  const getStatusColor = (): string => {
    if (!networkConnected) {
      return '#F44336'; // 빨간색 - 네트워크 없음
    }
    
    switch (connectionStatus) {
      case 'CONNECTED': return '#4CAF50'; // 초록색 - 연결됨
      case 'CONNECTING': return '#FF9800'; // 주황색 - 연결 중
      case 'ERROR': return '#F44336'; // 빨간색 - 연결 실패
      default: return '#9E9E9E'; // 회색 - 연결 끊김
    }
  };

  const renderMessage = (message: ChatMessage & { _isMyMessage?: boolean }, index: number) => {
    if (
      message.messageType === 'USER_JOIN' ||
      message.messageType === 'USER_LEAVE'
    ) {
      const systemMsg = message as SystemMessage;
      const systemText =
        systemMsg.messageType === 'USER_JOIN'
          ? `${systemMsg.userName || '사용자'}님이 입장했습니다.`
          : `${systemMsg.userName || '사용자'}님이 퇴장했습니다.`;

      return (
        <View key={index} style={styles.systemMessageContainer}>
          <Text style={styles.systemMessageText}>{systemText}</Text>
        </View>
      );
    }

    const isMyMessage = 'userId' in message && message.userId === currentUserId;

    return (
      <View key={index} style={[
        styles.messageContainer,
        isMyMessage ? styles.myMessageContainer : styles.otherMessageContainer
      ]}>
        {!isMyMessage && 'nickname' in message && (
          <Text style={styles.nicknameText}>{message.nickname}</Text>
        )}
        
        <View style={[
          styles.messageBubble,
          isMyMessage ? styles.myMessageBubble : styles.otherMessageBubble
        ]}>
          <Text style={[
            styles.messageText,
            isMyMessage ? styles.myMessageText : styles.otherMessageText
          ]}>
            {message.content}
          </Text>
          <Text style={[
            styles.timeText,
            isMyMessage ? styles.myTimeText : styles.otherTimeText
          ]}>
            {formatTime(message.timestamp)}
          </Text>
        </View>
      </View>
    );
  };

  useEffect(() => {
    console.log(`📱 MatchChatRoomScreen 마운트됨 [${componentId}] - WebSocket 연결 시작`, { 
      websocketUrl, 
      sessionToken: sessionToken?.substring(0, 10) + '...' 
    });
    
    // 개발 모드에서 중복 마운트 방지
    let isMounted = true;
    
    const timer = setTimeout(() => {
      if (isMounted) {
        connectToWebSocket();
      }
    }, 100);
    
    return () => {
      console.log(`📱 MatchChatRoomScreen 언마운트됨 [${componentId}] - WebSocket 연결 정리`);
      isMounted = false;
      clearTimeout(timer);
      disconnect();
    };
  }, []);

  // 앱 상태 변화 감지 (ws dependency 제거하여 무한 루프 방지)
  useEffect(() => {
    const handleAppStateChange = (nextAppState: string) => {
      console.log('📱 앱 상태 변화:', appState, '→', nextAppState);
      
      if (appState.match(/inactive|background/) && nextAppState === 'active') {
        console.log('📱 백그라운드에서 복귀 - WebSocket 상태 확인');
        // 현재 연결 상태 확인 후 재연결 결정
        if (connectionStatus !== 'CONNECTED' && reconnectAttempts < maxReconnectAttempts && !isReconnecting) {
          console.log('📱 백그라운드 복귀 시 WebSocket 재연결 시도');
          setTimeout(() => {
            connectToWebSocket();
          }, 1000);
        }
      } else if (nextAppState.match(/inactive|background/)) {
        console.log('📱 백그라운드로 이동');
        // 백그라운드 이동 시에는 연결 상태만 기록
        if (connectionStatus === 'CONNECTED') {
          console.log('📱 백그라운드 이동 시 연결 상태 유지');
        }
      }
      
      setAppState(nextAppState);
    };

    const subscription = AppState.addEventListener('change', handleAppStateChange);
    return () => subscription?.remove();
  }, [appState, connectionStatus, reconnectAttempts, isReconnecting]);

  useEffect(() => {
    const unsubscribeBeforeRemove = navigation.addListener('beforeRemove', () => {
      console.log('📱 화면 제거 직전 - WebSocket 연결 해제');
      disconnect();
    });

    // 화면 포커스/블러 이벤트 추가
    const unsubscribeFocus = navigation.addListener('focus', () => {
      console.log('📱 MatchChatRoom 화면 포커스됨');
    });

    const unsubscribeBlur = navigation.addListener('blur', () => {
      console.log('📱 MatchChatRoom 화면 블러됨 - WebSocket 연결 해제');
      disconnect();
    });

    return () => {
      unsubscribeBeforeRemove();
      unsubscribeFocus();
      unsubscribeBlur();
    };
  }, [navigation]);

  // 네트워크 상태 모니터링
  useEffect(() => {
    const unsubscribe = NetInfo.addEventListener(state => {
      const isConnected = state.isConnected ?? false;
      console.log('🌐 네트워크 상태 변화:', { 
        isConnected, 
        type: state.type, 
        isInternetReachable: state.isInternetReachable 
      });
      
      setNetworkConnected(isConnected);
      
      if (!isConnected) {
        // 네트워크가 끊어지면 WebSocket 연결 해제
        console.log('🌐 네트워크 연결 끊어짐 - WebSocket 연결 해제');
        if (ws) {
          ws.close(1000, 'Network disconnected');
        }
        setConnectionStatus('ERROR');
      } else if (networkConnected === false && isConnected) {
        // 네트워크가 다시 연결되면 재연결 시도 (단, 화면이 활성 상태일 때만)
        console.log('🌐 네트워크 다시 연결됨 - WebSocket 재연결 시도');
        if (appState === 'active' && reconnectAttempts < maxReconnectAttempts) {
          setTimeout(() => {
            connectToWebSocket();
          }, 2000); // 2초 후 재연결 시도
        }
      }
    });

    return () => unsubscribe();
  }, [appState, reconnectAttempts, networkConnected, ws]);

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <KeyboardAvoidingView 
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <View style={[styles.header, { backgroundColor: themeColor }]}>
          <TouchableOpacity onPress={() => {
            if (navigation.canGoBack()) {
              navigation.goBack();
            }
          }}>
            <Text style={styles.backButton}>← 나가기</Text>
          </TouchableOpacity>
          <Text style={styles.title}>
            {isWatchChat ? '직관채팅' : room.matchTitle}
          </Text>
          <View style={styles.statusContainer}>
            <View style={[
              styles.statusIndicator,
              { backgroundColor: getStatusColor() }
            ]} />
            <Text style={styles.statusText}>
              {!networkConnected ? '네트워크 없음' :
               connectionStatus === 'CONNECTED' ? '연결됨' : 
               connectionStatus === 'CONNECTING' ? '연결 중...' :
               connectionStatus === 'ERROR' ? '연결 실패' : '연결 끊김'}
            </Text>
          </View>
        </View>

        <ScrollView 
          ref={scrollViewRef}
          style={styles.messagesContainer}
          contentContainerStyle={styles.messagesContent}
          showsVerticalScrollIndicator={false}
        >
          {messages.length === 0 ? (
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>
                {connectionStatus === 'CONNECTED' 
                  ? '아직 메시지가 없습니다.\n첫 번째 메시지를 보내보세요!' 
                  : connectionStatus === 'ERROR'
                  ? '서버에 연결할 수 없습니다.\n네트워크 상태를 확인하거나\n나중에 다시 시도해주세요.'
                  : '채팅방에 연결중입니다...'}
              </Text>
            </View>
          ) : (
            messages.map(renderMessage)
          )}
        </ScrollView>

        {connectionStatus === 'CONNECTED' && (
          <View style={styles.inputContainer}>
            <TextInput
              style={styles.textInput}
              value={currentMessage}
              onChangeText={setCurrentMessage}
              placeholder="메시지를 입력하세요..."
              placeholderTextColor="#999"
              multiline={true}
              maxLength={500}
              onSubmitEditing={sendMessage}
              blurOnSubmit={false}
            />
            <TouchableOpacity 
              style={[
                styles.sendButton,
                !currentMessage.trim() && styles.sendButtonDisabled
              ]}
              onPress={sendMessage}
              disabled={!currentMessage.trim()}
            >
              <Text style={styles.sendButtonText}>전송</Text>
            </TouchableOpacity>
          </View>
        )}
      </KeyboardAvoidingView>
    </View>
  );
};

