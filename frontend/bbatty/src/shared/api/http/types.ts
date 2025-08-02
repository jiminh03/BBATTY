export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  errorMessage?: string;
}

export interface AuthRequest {
  chatType: string;
  roomId: string;
}

export interface AuthResponse {
  success: boolean;
  sessionToken?: string;
  errorMessage?: string;
}