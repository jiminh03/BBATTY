import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
  SafeAreaView,
  Alert,
  TextInput,
  KeyboardAvoidingView,
  Platform,
  AppState,
} from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import type { RouteProp } from '@react-navigation/native';
import { useMatchChatWebSocket } from '../../features/match-chat';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { ChatMessage, MatchChatMessage, SystemMessage } from '../../features/match-chat';
import type { ChatStackParamList } from '../../navigation/types';
import { useUserStore } from '../../entities/user/model/userStore';
import { useThemeColor } from '../../shared/team/ThemeContext';

type NavigationProp = StackNavigationProp<ChatStackParamList>;
type RoutePropType = RouteProp<ChatStackParamList, 'MatchChatRoom'>;

export const MatchChatRoomScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<RoutePropType>();
  const { room, websocketUrl, sessionToken } = route.params;
  const themeColor = useThemeColor();
  
  // 워치채팅 여부 확인
  const isWatchChat = websocketUrl.includes('/ws/watch-chat/') || (websocketUrl.includes('gameId=') && websocketUrl.includes('teamId='));
  
  console.log('MatchChatRoomScreen route.params:', route.params);
  
  const [currentMessage, setCurrentMessage] = useState('');
  const getCurrentUser = useUserStore((state) => state.getCurrentUser);
  const currentUser = getCurrentUser();
  const currentUserId = currentUser?.userId || 45; // fallback to test ID that matches log
  
  console.log('Current User:', currentUser);
  console.log('Current User ID:', currentUserId);
  
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

  const connectToWebSocket = () => {
    try {
      setConnectionStatus('CONNECTING');
      
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
      if (wsUrl && wsUrl.includes('i13a403.p.ssafy.io:8084') && sessionToken && sessionToken.startsWith('mock_session_token')) {
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
        console.log('웹소켓 연결 성공');
        
        // 매치채팅과 직관채팅 모두 사용자 정보 전송
        const isWatchChat = wsUrl.includes('/ws/watch-chat/') || (wsUrl.includes('gameId=') && wsUrl.includes('teamId='));
        
        let authData;
        if (isWatchChat) {
          // 직관채팅용 인증 데이터
          authData = {
            gameId: room.gameId || '1258',
            teamId: currentUser?.teamId || 3,
            nickname: currentUser?.nickname || 'Anonymous',
            userId: currentUser?.userId || currentUserId
          };
        } else {
          // 매치채팅용 인증 데이터
          authData = {
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
            // 서버 버그로 인해 JSON 객체 자체가 메시지 내용으로 오는 경우 필터링
            const content = messageData.content || '';
            const isJsonMessage = typeof content === 'string' && (
              content.startsWith('{') || 
              content.includes('"messageType"') ||
              content.includes('"nickname"') ||
              content.includes('"userId"') ||
              content.includes('"roomId"') ||
              content.includes('"timestamp"')
            );
              
            if (!isJsonMessage) {
              addMessage(messageData, isMyMessage);
              console.log('✅ 정상 메시지 추가:', content);
            } else {
              console.log('🚫 JSON 객체 메시지 필터링됨:', content);
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
        
        // 정상 종료(1000)가 아닌 경우 재연결 시도
        if (event.code !== 1000 && !isReconnecting && appState === 'active') {
          setIsReconnecting(true);
          console.log('🔄 비정상 종료로 인한 재연결 시도...');
          setTimeout(() => {
            if (appState === 'active') {
              connectToWebSocket();
            }
            setIsReconnecting(false);
          }, 3000);
        }
      };

      websocket.onerror = (error) => {
        setConnectionStatus('ERROR');
        console.error('웹소켓 오류:', error);
        console.log('웹소켓 오류 상세:', JSON.stringify(error, null, 2));
        
        // 재연결 중이 아니고 앱이 활성 상태일 때만 재연결 시도
        if (!isReconnecting && appState === 'active') {
          setIsReconnecting(true);
          setTimeout(() => {
            if (appState === 'active') {
              console.log('🔄 에러로 인한 재연결 시도...');
              connectToWebSocket();
            }
            setIsReconnecting(false);
          }, 3000);
        }
      };

    } catch (error) {
      console.error('웹소켓 연결 오류:', error);
      setConnectionStatus('ERROR');
    }
  };

  const disconnect = () => {
    if (ws) {
      ws.close();
      setWs(null);
      setConnectionStatus('DISCONNECTED');
      setSentMessages(new Set());
      console.log('웹소켓 연결 해제');
    }
  };

  const sendMessage = () => {
    if (!currentMessage.trim()) {
      Alert.alert('알림', '메시지를 입력해주세요.');
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
    switch (connectionStatus) {
      case 'CONNECTED': return '#4CAF50';
      case 'CONNECTING': return '#FF9800';
      case 'ERROR': return '#F44336';
      default: return '#9E9E9E';
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
    connectToWebSocket();
    
    return () => {
      disconnect();
    };
  }, []);

  // 앱 상태 변화 감지
  useEffect(() => {
    const handleAppStateChange = (nextAppState: string) => {
      console.log('📱 앱 상태 변화:', appState, '→', nextAppState);
      
      if (appState.match(/inactive|background/) && nextAppState === 'active') {
        console.log('📱 백그라운드에서 복귀 - WebSocket 재연결 시도');
        // 백그라운드에서 포그라운드로 돌아왔을 때
        if (ws && ws.readyState !== WebSocket.OPEN) {
          setTimeout(() => {
            connectToWebSocket();
          }, 1000);
        }
      } else if (nextAppState.match(/inactive|background/)) {
        console.log('📱 백그라운드로 이동 - WebSocket 연결 정리');
        // 백그라운드로 갈 때 연결 정리
        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.close(1000, 'App going to background');
        }
      }
      
      setAppState(nextAppState);
    };

    const subscription = AppState.addEventListener('change', handleAppStateChange);
    return () => subscription?.remove();
  }, [appState, ws]);

  useEffect(() => {
    const unsubscribe = navigation.addListener('beforeRemove', () => {
      disconnect();
    });

    return unsubscribe;
  }, [navigation]);

  return (
    <SafeAreaView style={styles.container}>
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
            <Text style={styles.statusText}>{connectionStatus}</Text>
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
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 16,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  backButton: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  title: {
    fontSize: 20,
    fontWeight: '900',
    color: '#ffffff',
    flex: 1,
    textAlign: 'center',
    marginHorizontal: 16,
    textShadowColor: 'rgba(0,0,0,0.3)',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 2,
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusIndicator: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 6,
  },
  statusText: {
    fontSize: 12,
    color: '#ffffff',
    fontWeight: '500',
  },
  messagesContainer: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  messagesContent: {
    padding: 16,
    flexGrow: 1,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 40,
  },
  emptyText: {
    fontSize: 16,
    color: '#999',
    textAlign: 'center',
    lineHeight: 24,
  },
  messageContainer: {
    marginBottom: 12,
    maxWidth: '80%',
  },
  myMessageContainer: {
    alignSelf: 'flex-end',
    alignItems: 'flex-end',
  },
  otherMessageContainer: {
    alignSelf: 'flex-start',
    alignItems: 'flex-start',
  },
  nicknameText: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
    marginLeft: 4,
  },
  messageBubble: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 18,
    maxWidth: '100%',
  },
  myMessageBubble: {
    backgroundColor: '#007AFF',
    borderBottomRightRadius: 4,
  },
  otherMessageBubble: {
    backgroundColor: '#fff',
    borderBottomLeftRadius: 4,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  messageText: {
    fontSize: 16,
    lineHeight: 20,
    marginBottom: 4,
  },
  myMessageText: {
    color: '#fff',
  },
  otherMessageText: {
    color: '#333',
  },
  timeText: {
    fontSize: 10,
    alignSelf: 'flex-end',
  },
  myTimeText: {
    color: 'rgba(255, 255, 255, 0.7)',
  },
  otherTimeText: {
    color: '#999',
  },
  systemMessageContainer: {
    alignItems: 'center',
    marginVertical: 8,
  },
  systemMessageText: {
    fontSize: 12,
    color: '#999',
    backgroundColor: 'rgba(0, 0, 0, 0.05)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 12,
  },
  inputContainer: {
    flexDirection: 'row',
    padding: 16,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#eee',
    alignItems: 'flex-end',
  },
  textInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 10,
    fontSize: 16,
    maxHeight: 80,
    marginRight: 8,
    backgroundColor: '#f8f9fa',
  },
  sendButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sendButtonDisabled: {
    backgroundColor: '#ccc',
  },
  sendButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 14,
  },
});