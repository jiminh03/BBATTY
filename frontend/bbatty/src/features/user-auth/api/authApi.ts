import { apiClient } from '../../../shared';
import { wrapApiCall, ApiError } from '../../../shared/api/utils/apiWrapper';
import { AsyncResult } from '../../../shared/utils/result';
import { RegisterRequest, RegisterResponse, LoginRequest, LoginResponse } from '../model/authTypes';

export const authApi = {
  signup: (request: RegisterRequest): AsyncResult<RegisterResponse, ApiError> =>
    wrapApiCall(() => apiClient.post('/api/auth/s ignup', request)),

  login: (request: LoginRequest): AsyncResult<LoginResponse, ApiError> =>
    wrapApiCall(() => apiClient.post('/api/auth/login', request)),

  logout: (): AsyncResult<void, ApiError> => wrapApiCall(() => apiClient.post('/api/auth/logout')),
};
