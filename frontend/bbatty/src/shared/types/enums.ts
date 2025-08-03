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
