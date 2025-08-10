import { apiClient } from '../../../shared';
import { wrapApiCall, ApiError } from '../../../shared/api/utils/apiWrapper';
import { AsyncResult } from '../../../shared/utils/result';
import { DetailedUserStats, DirectViewRecord, Season } from '../model/statsTypes';

export const userStatsApi = {
  getUserStats: (userId: number, season: Season): AsyncResult<DetailedUserStats, ApiError> =>
    wrapApiCall(() => apiClient.get(`/api/users/${userId}/stats`, { params: { season } })),

  getDirectViewHistory: (userId: number, season: Season): AsyncResult<DirectViewRecord[], ApiError> =>
    wrapApiCall(() => apiClient.get(`/api/users/${userId}/direct-views`, { params: { season } })),
};
