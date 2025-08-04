// 사용 예시
// const config: SocketConfig = {
//   url: 'ws://localhost:8084/ws/game-chat',
//   options: {
//     auth: { sessionToken: 'token123' },
//     query: { 
//       chatType: ChatType.WATCH,
//       roomType: ChatRoomType.GAME 
//     }
//   }
// };

// 이벤트 처리
// client.on(MessageType.USER_JOIN, (data) => {
//   console.log('사용자 입장:', data);
// });

// client.on(MessageType.CHAT, (data) => {
//   console.log('채팅 메시지:', data);
// });

export enum ChatType {
  WATCH = 'WATCH',
  MATCH = 'MATCH',
}

export enum ChatRoomType {
  GAME = 'game',
  MATCH = 'match',
}

export enum MessageType {
  CHAT = 'chat',
  SYSTEM = 'system',
  ERROR = 'error',
  USER_JOIN = 'user_join',
  USER_LEAVE = 'user_leave',
  USER_ACTIVITY = 'user_activity',
}

export enum UserStatus {
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  BLOCKED = 'blocked',
}
