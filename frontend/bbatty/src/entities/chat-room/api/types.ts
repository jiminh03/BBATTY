export interface AuthRequest {
  /** JWT 토큰 */
  accessToken: string;
  /** 채팅방 타입 (game, match) */
  chatType: string;
  /** 채팅방 ID (teamId 또는 matchId) */
  roomId: string;
}

export interface AuthResponse {
  /** 세션 토큰 (Chat 서버에서 생성) */
  sessionToken: string;
  /** WebSocket 연결 URL */
  websocketUrl: string;
  /** 인증 성공 여부 */
  success: boolean;
  /** 오류 메시지 */
  errorMessage?: string;
  /** 세션 만료 시간 (초) */
  expiresIn: number;
}