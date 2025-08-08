import { create } from 'zustand';
import { KakaoLoginResponse } from '../../auth';

interface AuthStore {
  kakaoUserInfo: KakaoLoginResponse | null;
  kakaoAccessToken: string | null;
  setKakaoUserInfo: (userInfo: KakaoLoginResponse) => void;
  setKakaoAccessToken: (token: string) => void;
  clearKakaoUserInfo: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  kakaoUserInfo: null,
  kakaoAccessToken: null,
  setKakaoUserInfo: (userInfo) => set({ kakaoUserInfo: userInfo }),
  setKakaoAccessToken: (token) => set({ kakaoAccessToken: token }),
  clearKakaoUserInfo: () => set({ kakaoUserInfo: null, kakaoAccessToken: null }),
}));
