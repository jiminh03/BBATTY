import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { QueryKeys, QueryInvalidator } from '../../../shared/api/lib/tanstack/queryKeys';
import { useApi, useApiMutation } from '../../../shared';
import { useAuthStore } from '../model/authStore';
import { authApi } from '../api/authApi';
import type { CheckNicknameRequest } from '../model/types';

const authKeys = {
  currentUser: () => ['auth', 'currentUser'],
  checkNickname: (nickname: string) => ['auth', 'checkNickname', nickname],
};

// 현재 사용자 정보 조회
export const useCurrentUser = () => {
  return useApi({
    queryKey: authKeys.currentUser(),
    apiFunction: () => authApi.fetchCurrentUser(),
    staleTime: 10 * 60 * 1000, // 10분
  });
};

// // 프로필 업데이트
// export const useUploadImage = () => {
//   const queryClient = useQueryClient();
//   const updateUser = useAuthStore((state) => state.updateUser);

//   return useApiMutation({
//     apiFunction: authApi.uploadImage,
//     onSuccess: (data) => {
//       updateUser(data);
//       queryClient.setQueryData(authKeys.currentUser(), data);
//       QueryInvalidator.invalidateEntity(queryClient, 'auth');
//     },
//   });
// };

// 닉네임 중복 확인
export const useCheckNickname = (nickname: string, options?: { enabled?: boolean }) => {
  return useApi({
    queryKey: authKeys.checkNickname(nickname),
    apiFunction: () => authApi.checkNickname({ nickname }),
    enabled: options?.enabled ?? false, // 명시적으로 활성화할 때만 실행
    staleTime: 30 * 1000, // 30초
    retry: false,
  });
};

// 닉네임 중복 확인 (mutation 버전 - 즉시 실행용)
export const useCheckNicknameMutation = () => {
  return useApiMutation({
    apiFunction: authApi.checkNickname,
  });
};

// // 회원 탈퇴
// export const useDeleteAccount = () => {
//   const queryClient = useQueryClient();
//   const clearAuth = useAuthStore((state) => state.clearAuth);

//   return useApiMutation({
//     apiFunction: authApi.deleteAccount,
//     onSuccess: () => {
//       clearAuth();
//       queryClient.clear(); // 모든 캐시 초기화
//     },
//   });
// };

// 토큰 갱신 훅
// export const useRefreshToken = () => {
//   return useApiMutation({
//     apiFunction: authApi.refreshToken,
//     onSuccess: async (data) => {
//       const { accessToken } = data;
//       const { tokenManager, updateClientsToken } = await import('@/shared/api');

//       await tokenManager.setToken(accessToken);
//       await updateClientsToken(accessToken);
//     },
//     onError: () => {
//       // 토큰 갱신 실패 시 로그아웃
//       useAuthStore.getState().clearAuth();
//     },
//   });
// };
