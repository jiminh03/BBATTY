import { useQuery } from '@tanstack/react-query';
import { userStatsApi } from '../api/userStatsApi';
import { Season } from '../model/statsTypes';
import { isOk } from '../../../shared/utils/result';

export const useUserStats = (userId: number, season: Season = '전체') => {
  const {
    data: stats,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['userStats', userId, season],
    queryFn: async () => {
      const result = await userStatsApi.getUserStats(userId, season);
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

export const useDirectViewHistory = (userId: number, season: Season = '전체') => {
  const {
    data: records,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['directViewHistory', userId, season],
    queryFn: async () => {
      const result = await userStatsApi.getDirectViewHistory(userId, season);
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
