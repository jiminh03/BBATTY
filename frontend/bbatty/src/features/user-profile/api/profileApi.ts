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
  getProfile: (userId?: number): AsyncResult<UserProfile, ApiError> =>
    wrapApiCall(() =>
      apiClient.get('/api/profile', {
        params: userId ? { userId } : undefined,
      })
    ),

  updateProfile: (data: UpdateProfileRequest): AsyncResult<UserProfile, ApiError> =>
    wrapApiCall(() => apiClient.put('/api/profile/update', data)),

  updatePrivacySettings: (settings: UserPrivacySettings): AsyncResult<null, ApiError> => {
    return wrapApiCall(() => apiClient.put('/api/profile/privacy', settings));
  },

  checkNickname: (request: CheckNicknameRequest): AsyncResult<CheckNicknameResponse, ApiError> =>
    wrapApiCall(() => apiClient.get<CheckNicknameResponse>('/api/auth/check-nickname', { params: request })),
};
