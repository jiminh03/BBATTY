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
} from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import type { RouteProp } from '@react-navigation/native';
import { useMatchChatWebSocket } from '../../features/match-chat';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { ChatMessage, MatchChatMessage, SystemMessage } from '../../features/match-chat';
import type { ChatStackParamList } from '../../navigation/types';
import { useUserStore } from '../../entities/user/model/userStore';

type NavigationProp = StackNavigationProp<ChatStackParamList>;
type RoutePropType = RouteProp<ChatStackParamList, 'MatchChatRoom'>;

export const MatchChatRoomScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<RoutePropType>();
  const { room, websocketUrl, sessionToken } = route.params;
  
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
      
      console.log('웹소켓 연결 시작');
      console.log('websocketUrl:', websocketUrl);
      console.log('sessionToken:', sessionToken);
      console.log(`최종 wsUrl: ${wsUrl}`);

      if (!wsUrl) {
        console.error('웹소켓 URL이 없습니다!');
        setConnectionStatus('DISCONNECTED');
        return;
      }

      const websocket = new WebSocket(wsUrl);
      setWs(websocket);

      websocket.onopen = () => {
        setConnectionStatus('CONNECTED');
        console.log('웹소켓 연결 성공');
        
        // watch chat의 경우 사용자 정보 전송하지 않음
        const isWatchChat = wsUrl.includes('/ws/watch-chat/') || (wsUrl.includes('gameId=') && wsUrl.includes('teamId='));
        
        if (!isWatchChat) {
          // 매치 채팅의 경우만 사용자 인증 정보 전송
          const authData = {
            matchId: room.matchId,
            nickname: currentUser?.nickname || 'Anonymous',
            winRate: 75,
            profileImgUrl: currentUser?.profileImageURL || '',
            isWinFairy: false
          };
          
          websocket.send(JSON.stringify(authData));
        }
      };

      websocket.onmessage = (event) => {
        try {
          const messageData = JSON.parse(event.data);
          console.log('메시지 수신:', messageData);
          
          const messageKey = `${messageData.content}_${messageData.timestamp}`;
          const isMyMessage = sentMessages.has(messageKey);
          
          if (messageData.messageType === 'CHAT') {
            addMessage(messageData, isMyMessage);
            
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
      };

      websocket.onerror = (error) => {
        setConnectionStatus('ERROR');
        console.error('웹소켓 오류:', error);
        
        // WebSocket 서버가 실행되지 않은 경우 안내 메시지
        setTimeout(() => {
          Alert.alert(
            '채팅 서버 연결 실패', 
            'WebSocket 서버(8084 포트)가 실행되지 않았습니다.\n\n백엔드 채팅 서버를 먼저 실행해주세요.',
            [{ text: '확인', onPress: () => {
              if (navigation.canGoBack()) {
                navigation.goBack();
              }
            }}]
          );
        }, 1000);
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
        <View style={styles.header}>
          <TouchableOpacity onPress={() => {
            if (navigation.canGoBack()) {
              navigation.goBack();
            }
          }}>
            <Text style={styles.backButton}>← 나가기</Text>
          </TouchableOpacity>
          <Text style={styles.title}>{room.matchTitle}</Text>
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
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
    backgroundColor: '#f8f9fa',
  },
  backButton: {
    color: '#007AFF',
    fontSize: 16,
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    flex: 1,
    textAlign: 'center',
    marginHorizontal: 16,
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
    color: '#666',
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