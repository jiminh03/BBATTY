import { apiClient, Season } from '../../../shared';
import { wrapApiCall, ApiError } from '../../../shared/api/utils/apiWrapper';
import { AsyncResult } from '../../../shared/utils/result';
import {
  UserProfile,
  UpdateProfileRequest,
  UserPrivacySettings,
  CheckNicknameRequest,
  CheckNicknameResponse,
} from '../model/profileTypes';

export const profileApi = {
  // 1. 사용자 기본 프로필 조회 (/api/profile)
  getProfile: (userId?: number): AsyncResult<UserProfile, ApiError> =>
    wrapApiCall(() =>
      apiClient.get('/api/profile', {
        params: userId ? { userId } : undefined,
      })
    ),

  // 2. 프로필 수정 (/api/profile/update)
  updateProfile: (data: UpdateProfileRequest): AsyncResult<UserProfile, ApiError> =>
    wrapApiCall(() => apiClient.put('/api/profile/update', data)),

  // 3. 프라이버시 설정 업데이트 (/api/profile/privacy)
  updatePrivacySettings: (settings: UserPrivacySettings): AsyncResult<null, ApiError> =>
    wrapApiCall(() => apiClient.put('/api/profile/privacy', settings)),

  checkNickname: (request: CheckNicknameRequest): AsyncResult<CheckNicknameResponse, ApiError> =>
    wrapApiCall(() => apiClient.get<CheckNicknameResponse>('/api/auth/check-nickname', { params: request })),
};
