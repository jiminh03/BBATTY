import { useConnectionStore } from '../../chat-connection/model/store';
import { SendMessageRequest } from '../model/types';

export const sendMessageApi = {
  // WebSocket을 통한 메시지 전송
  sendChatMessage: async (request: SendMessageRequest): Promise<void> => {
    const { client } = useConnectionStore.getState();
    
    if (!client || !client.getConnectionStatus()) {
      throw new Error('채팅 서버에 연결되지 않았습니다.');
    }

    try {
      // 단순 텍스트 메시지 전송
      if (request.messageType === 'CHAT' || !request.messageType) {
        client.sendChatMessage(request.content);
      } else {
        // 구조화된 메시지 전송 (시스템 메시지 등)
        client.emit('message', {
          type: request.messageType,
          content: request.content,
          roomId: request.roomId,
          metadata: request.metadata,
        });
      }
    } catch (error) {
      throw new Error(`메시지 전송 실패: ${error instanceof Error ? error.message : '알 수 없는 오류'}`);
    }
  },

  // 방 입장 메시지
  sendJoinRoom: async (roomId: string, userData?: any): Promise<void> => {
    const { client } = useConnectionStore.getState();
    
    if (!client || !client.getConnectionStatus()) {
      throw new Error('채팅 서버에 연결되지 않았습니다.');
    }

    client.joinRoom(roomId, userData);
  },

  // 방 퇴장 메시지
  sendLeaveRoom: async (roomId: string): Promise<void> => {
    const { client } = useConnectionStore.getState();
    
    if (!client || !client.getConnectionStatus()) {
      throw new Error('채팅 서버에 연결되지 않았습니다.');
    }

    client.leaveRoom(roomId);
  },
};
