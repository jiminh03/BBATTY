import { apiClient } from '../../../shared';
import { wrapApiCall, ApiError } from '../../../shared/api/utils/apiWrapper';
import { AsyncResult } from '../../../shared/utils/result';
import { CheckNicknameRequest, CheckNicknameResponse } from '../model/profileTypes';

export const profileApi = {
  checkNickname: (request: CheckNicknameRequest): AsyncResult<CheckNicknameResponse, ApiError> =>
    wrapApiCall(() => apiClient.get<CheckNicknameResponse>('/api/auth/check-nickname', { params: request })),
};
