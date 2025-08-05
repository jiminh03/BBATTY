// ================================== Request =======================================

export interface CheckNicknameRequest {
  nickname: string;
}

// ================================== Response =======================================

// 닉네임 중복 확인 응답
export interface CheckNicknameResponse {
  isAvailable: boolean;
  message: string;
}
