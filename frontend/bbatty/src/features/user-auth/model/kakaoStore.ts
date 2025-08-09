import { create } from 'zustand';
import { KakaoLoginResponse } from '..';

interface KakaoStore {
  kakaoUserInfo: KakaoLoginResponse | null;
  kakaoAccessToken: string | null;
  setKakaoUserInfo: (userInfo: KakaoLoginResponse) => void;
  setKakaoAccessToken: (token: string) => void;
  clearKakaoUserInfo: () => void;
}

export const usekakaoStore = create<KakaoStore>((set) => ({
  kakaoUserInfo: null,
  kakaoAccessToken: null,
  setKakaoUserInfo: (userInfo) => set({ kakaoUserInfo: userInfo }),
  setKakaoAccessToken: (token) => set({ kakaoAccessToken: token }),
  clearKakaoUserInfo: () => set({ kakaoUserInfo: null, kakaoAccessToken: null }),
}));
