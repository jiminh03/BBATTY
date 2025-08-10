import { apiClient } from '../../../shared';
import { wrapApiCall, ApiError } from '../../../shared/api/utils/apiWrapper';
import { AsyncResult } from '../../../shared/utils/result';
import {
  UserProfile,
  CheckNicknameRequest,
  CheckNicknameResponse,
  UpdateProfileRequest,
  UserPrivacySettings,
} from '../model/profileTypes';

export const profileApi = {
  // 프로필 조회 (본인 또는 타인)
  getProfile: (userId?: number): AsyncResult<UserProfile, ApiError> =>
    wrapApiCall(() => (userId ? apiClient.get(`/api/users/${userId}/profile`) : apiClient.get('/api/user/profile'))),

  // 프로필 수정 (본인만)
  updateProfile: (data: UpdateProfileRequest): AsyncResult<UserProfile, ApiError> =>
    wrapApiCall(() => apiClient.put('/api/user/profile', data)),

  // 프로필 이미지 업로드
  uploadProfileImage: (imageFile: FormData): AsyncResult<{ imageUrl: string }, ApiError> =>
    wrapApiCall(() =>
      apiClient.post('/api/user/profile/image', imageFile, {
        headers: { 'Content-Type': 'multipart/form-data' } as any,
      })
    ),

  updatePrivacySettings: (settings: UserPrivacySettings): AsyncResult<UserPrivacySettings, ApiError> =>
    wrapApiCall(() => apiClient.put('/api/user/privacy-settings', settings)),

  getPrivacySettings: (): AsyncResult<UserPrivacySettings, ApiError> =>
    wrapApiCall(() => apiClient.get('/api/user/privacy-settings')),

  checkNickname: (request: CheckNicknameRequest): AsyncResult<CheckNicknameResponse, ApiError> =>
    wrapApiCall(() => apiClient.get<CheckNicknameResponse>('/api/auth/check-nickname', { params: request })),
};
