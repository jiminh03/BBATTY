import { useState, useEffect, useRef, useCallback } from 'react';

export interface BaseMessage {
  messageType: string;
  roomId: string;
  content: string;
  timestamp: string;
}

export interface MatchChatMessage extends BaseMessage {
  messageType: 'CHAT';
  userId: string;
  nickname: string;
  profileImgUrl?: string;
  winFair?: boolean;
}

export interface SystemMessage extends BaseMessage {
  type: 'user_join' | 'user_leave';
  userId?: string;
  userName?: string;
  messageType: 'USER_JOIN' | 'USER_LEAVE';
  kafkaTimestamp?: number;
}

export type ChatMessage = MatchChatMessage | SystemMessage;

export type ConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

interface UseMatchChatWebSocketProps {
  websocketUrl: string;
  currentUserId: string;
  userNickname: string;
  profileImgUrl?: string;
  onMessage?: (message: ChatMessage) => void;
  onConnectionStatusChange?: (status: ConnectionStatus) => void;
}

export const useMatchChatWebSocket = ({
  websocketUrl,
  currentUserId,
  userNickname,
  profileImgUrl,
  onMessage,
  onConnectionStatusChange
}: UseMatchChatWebSocketProps) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('DISCONNECTED');
  const [sentMessages, setSentMessages] = useState<Set<string>>(new Set());
  
  const wsRef = useRef<WebSocket | null>(null);

  const updateConnectionStatus = useCallback((status: ConnectionStatus) => {
    setConnectionStatus(status);
    onConnectionStatusChange?.(status);
  }, [onConnectionStatusChange]);

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
      
      // 최대 100개 메시지만 유지
      if (newMessages.length > 100) {
        return newMessages.slice(-100);
      }
      return newMessages;
    });
    
    onMessage?.(message);
  }, [onMessage]);

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      return;
    }

    try {
      updateConnectionStatus('CONNECTING');
      
      let wsUrl = websocketUrl;
      
      console.log(`웹소켓 연결: ${wsUrl}`);

      const websocket = new WebSocket(wsUrl);
      wsRef.current = websocket;

      websocket.onopen = () => {
        updateConnectionStatus('CONNECTED');
        console.log('웹소켓 연결 성공');
        
        // 서버에 사용자 인증 정보 전송
        const matchId = new URL(wsUrl).searchParams.get('matchId');
        const authData = {
          matchId: matchId,
          nickname: userNickname,
          winRate: 75, // TODO: 실제 승률 데이터로 대체
          profileImgUrl: profileImgUrl || '',
          isWinFairy: false // TODO: 실제 승부요정 여부로 대체
        };
        
        websocket.send(JSON.stringify(authData));
        console.log('사용자 인증 정보 전송:', authData);
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
        updateConnectionStatus('DISCONNECTED');
        console.log(`웹소켓 연결 종료: ${event.code} - ${event.reason}`);
      };

      websocket.onerror = (error) => {
        updateConnectionStatus('ERROR');
        console.error('웹소켓 오류:', error);
      };

    } catch (error) {
      console.error('웹소켓 연결 오류:', error);
      updateConnectionStatus('ERROR');
    }
  }, [websocketUrl, sentMessages, addMessage, updateConnectionStatus]);

  const disconnect = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
      updateConnectionStatus('DISCONNECTED');
      setSentMessages(new Set());
      console.log('웹소켓 연결 해제');
    }
  }, [updateConnectionStatus]);

  const sendMessage = useCallback((content: string): Promise<boolean> => {
    return new Promise((resolve, reject) => {
      if (!content.trim()) {
        reject(new Error('메시지 내용이 비어있습니다.'));
        return;
      }

      if (wsRef.current?.readyState === WebSocket.OPEN) {
        try {
          const messageContent = content.trim();
          const timestamp = new Date().toISOString();
          
          const messageKey = `${messageContent}_${timestamp}`;
          setSentMessages(prev => new Set([...prev, messageKey]));
          
          wsRef.current.send(messageContent);
          console.log('메시지 전송:', messageContent);
          resolve(true);
        } catch (error) {
          console.error('메시지 전송 오류:', error);
          reject(new Error('메시지 전송에 실패했습니다.'));
        }
      } else {
        reject(new Error('웹소켓이 연결되지 않았습니다.'));
      }
    });
  }, []);

  const clearMessages = useCallback(() => {
    setMessages([]);
  }, []);

  // 컴포넌트 언마운트 시 연결 해제
  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  return {
    messages,
    connectionStatus,
    connect,
    disconnect,
    sendMessage,
    clearMessages,
    isConnected: connectionStatus === 'CONNECTED',
  };
};