import { useQuery } from '@tanstack/react-query';
import { profileApi } from '../api/profileApi';
import { isOk } from '../../../shared/utils/result';

export const useProfile = (userId?: number) => {
  const {
    data: profile,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['userProfile', userId],
    queryFn: async () => {
      const result = await profileApi.getProfile(userId);
      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
    staleTime: 5 * 60 * 1000, // 5분
  });

  return {
    profile,
    isLoading,
    error,
    refetch,
  };
};
