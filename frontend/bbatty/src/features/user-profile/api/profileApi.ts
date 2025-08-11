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
import { UserBadges } from '../model/badgeTypes';
import { StatsType, BasicStats } from '../model/statsTypes';

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

  // 4. 뱃지 조회 (/api/profile/badges)
  getBadges: (userId?: number, season?: Season): AsyncResult<UserBadges, ApiError> =>
    wrapApiCall(() =>
      apiClient.get('/api/profile/badges', {
        params: {
          ...(userId && { userId }),
          ...(season && season !== 'total' && { season }),
        },
      })
    ),

  // 5. 기본 승률 조회 (/api/profile/stats?type=basic)
  getBasicStats: (userId?: number, season?: Season): AsyncResult<BasicStats, ApiError> =>
    wrapApiCall(() =>
      apiClient.get('/api/profile/stats', {
        params: {
          ...(userId && { userId }),
          ...(season && { season }),
          type: 'basic',
        },
      })
    ),

  // 6. 상세 승률 조회 (/api/profile/stats?type=...)
  getDetailedStats: <T>(type: StatsType, userId?: number, season?: Season): AsyncResult<T, ApiError> =>
    wrapApiCall(() =>
      apiClient.get('/api/profile/stats', {
        params: {
          ...(userId && { userId }),
          ...(season && { season }),
          type,
        },
      })
    ),

  checkNickname: (request: CheckNicknameRequest): AsyncResult<CheckNicknameResponse, ApiError> =>
    wrapApiCall(() => apiClient.get<CheckNicknameResponse>('/api/auth/check-nickname', { params: request })),
};
