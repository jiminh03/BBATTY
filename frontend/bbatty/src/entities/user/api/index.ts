import { apiClient, uploadClient } from '../../../shared';
import type { User } from '../model/types';

export const userApi = {
  async getUserById(userId: string) {
    return apiClient.get<User>(`/users/${userId}`);
  },
};
