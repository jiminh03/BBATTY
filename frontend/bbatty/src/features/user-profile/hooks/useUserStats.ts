import { useQuery } from '@tanstack/react-query';
import { statsApi } from '../api/statsApi';
import { Season } from '../../../shared/utils/date';
import { isOk } from '../../../shared/utils/result';

export const useUserStats = (userId: number, season: Season = 'total') => {
  const {
    data: stats,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['userStats', userId, season],
    queryFn: async () => {
      const result = await statsApi.getBasicStats(userId, season);
      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
    staleTime: 5 * 60 * 1000, // 5분
  });

  return {
    stats,
    isLoading,
    error,
    refetch,
  };
};

export const useDirectViewHistory = (userId: number, season: Season = 'total') => {
  const {
    data: records,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['directViewHistory', userId, season],
    queryFn: async () => {
      const result = await statsApi.getDirectViewHistory(userId, season);
      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
    staleTime: 5 * 60 * 1000,
  });

  return {
    records,
    isLoading,
    error,
    refetch,
  };
};
