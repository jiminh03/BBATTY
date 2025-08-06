import { useConnectionStore } from '../../chat-connection/model/store';
import { SendMessageRequest } from '../model/types';

export const sendMessageApi = {
  sendChatMessage: async (request: SendMessageRequest): Promise<void> => {
    const { client, isConnected, roomId } = useConnectionStore.getState();
    
    if (!client || !isConnected) {
      throw new Error('채팅 서버에 연결되지 않았습니다.');
    }

    try {
      if (request.messageType === 'CHAT' || !request.messageType) {
        // 일반 채팅 메시지: 순수 텍스트로 전송
        client.sendChatMessage(request.content);
      } else {
        // 시스템 메시지: 구조화된 데이터로 전송
        client.emit('message', {
          type: request.messageType,
          content: request.content,
          roomId: roomId || request.roomId,
          metadata: request.metadata,
        });
      }
    } catch (error) {
      throw new Error(`메시지 전송 실패: ${error instanceof Error ? error.message : '알 수 없는 오류'}`);
    }
  },

  sendJoinRoom: async (roomId: string, userData?: any): Promise<void> => {
    const { client, isConnected } = useConnectionStore.getState();
    
    if (!client || !isConnected) {
      throw new Error('채팅 서버에 연결되지 않았습니다.');
    }

    client.joinRoom(roomId, userData);
  },

  sendLeaveRoom: async (roomId: string): Promise<void> => {
    const { client, isConnected } = useConnectionStore.getState();
    
    if (!client || !isConnected) {
      throw new Error('채팅 서버에 연결되지 않았습니다.');
    }

    client.leaveRoom(roomId);
  },
};