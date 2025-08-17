import { apiClient } from '../../../shared';
import { Season } from '../../../shared/utils/date';
import { wrapApiCall, ApiError } from '../../../shared/api/utils/apiWrapper';
import { AsyncResult } from '../../../shared/utils/result';
import { UserBadges } from '../model/badgeTypes';
import {
  StatsType,
  BasicStats,
  DetailedUserStats,
  DirectViewRecord,
  AttendanceRecordsResponse,
} from '../model/statsTypes';

export const statsApi = {
  getBadges: (userId?: number, season?: Season): AsyncResult<UserBadges, ApiError> =>
    wrapApiCall(() =>
      apiClient.get('/api/profile/badges', {
        params: {
          ...(userId && { userId }),
          ...(season && season !== 'total' && { season }),
        },
      })
    ),

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

  getDirectViewHistory: (userId: number, season?: Season): AsyncResult<DirectViewRecord[], ApiError> =>
    wrapApiCall(() =>
      apiClient.get(`/api/users/${userId}/direct-views`, {
        params: {
          ...(season && { season }),
        },
      })
    ),

  // 새로운 직관 기록 API (cursor 기반 페이지네이션)
  getAttendanceRecords: (params: {
    userId?: number;
    season?: Season;
    cursor?: number | null;
    limit?: number;
  }): AsyncResult<AttendanceRecordsResponse, ApiError> =>
    wrapApiCall(() =>
      apiClient.get('/api/profile/attendance-records', {
        params: {
          ...(params.userId && { userId: params.userId }),
          ...(params.season && { season: params.season }),
          ...(params.cursor && { cursor: params.cursor }),
          ...(params.limit && { limit: params.limit }),
        },
      })
    ),

  // 직관 년도 목록 조회 API
  getAttendanceYears: (userId?: number): AsyncResult<string[], ApiError> =>
    wrapApiCall(() =>
      apiClient.get('/api/profile/attendance-years', {
        params: {
          ...(userId && { userId }),
        },
      })
    ),
};
