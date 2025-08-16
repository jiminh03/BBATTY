import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { profileApi } from '../api/profileApi';
import { UserPrivacySettings, UpdateProfileRequest } from '../model/profileTypes';
import { Season } from '../../../shared';
import { StatsType } from '../model/statsTypes';
import { QueryKeys, QueryInvalidator } from '../../../shared/api/lib/tanstack/queryKeyTypes';
import { isOk } from '../../../shared/utils/result';
import { statsApi } from '../api/statsApi';

const PROFILE_ENTITY = 'profile';

// 프로필 조회
export const useProfile = (userId?: number) => {
  return useQuery({
    queryKey: userId ? QueryKeys.detail(PROFILE_ENTITY, userId) : QueryKeys.detail(PROFILE_ENTITY, 'me'),
    queryFn: async () => {
      const result = await profileApi.getProfile(userId);
      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
};

// 프로필 업데이트
export const useUpdateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: UpdateProfileRequest) => {
      const result = await profileApi.updateProfile(data);
      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(QueryKeys.detail(PROFILE_ENTITY, 'me'), updatedProfile);
      QueryInvalidator.invalidateEntity(queryClient, PROFILE_ENTITY);
    },
  });
};

// 프라이버시 설정 업데이트
export const useUpdatePrivacySettings = () => {
  return useMutation({
    mutationFn: async (settings: UserPrivacySettings) => {
      const result = await profileApi.updatePrivacySettings(settings);
      if (isOk(result)) {
        return settings;
      }
      throw new Error(result.error.message);
    },
    // 깜빡임 방지를 위해 캐시 업데이트 제거 (로컬 상태로 UI 관리)
  });
};

// 뱃지 조회
export const useUserBadges = (userId?: number, season?: Season) => {
  const params = {
    ...(userId && { userId }),
    ...(season && season !== 'total' && { season }),
  };

  return useQuery({
    queryKey: QueryKeys.badges(PROFILE_ENTITY, params),
    queryFn: async () => {
      const result = await statsApi.getBadges(userId, season);
      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
    staleTime: 5 * 60 * 1000,
  });
};

// 기본 승률 조회
export const useBasicStats = (userId?: number, season?: Season) => {
  const params = {
    type: 'basic' as const,
    ...(userId && { userId }),
    ...(season && { season }),
  };

  return useQuery({
    queryKey: QueryKeys.stats(PROFILE_ENTITY, 'basic', params),
    queryFn: async () => {
      const result = await statsApi.getBasicStats(userId, season);
      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
    staleTime: 3 * 60 * 1000,
  });
};

// 상세 승률 조회
export const useDetailedStats = <T = any>(
  type: StatsType,
  userId?: number,
  season?: Season,
  enabled: boolean = true
) => {
  const params = {
    type,
    ...(userId && { userId }),
    ...(season && { season }),
  };

  return useQuery({
    queryKey: QueryKeys.stats(PROFILE_ENTITY, type, params),
    queryFn: async () => {
      console.log('승률통계 : ', type, userId, season);
      const result = await statsApi.getDetailedStats<T>(type, userId, '2525');
      if (isOk(result)) {
        console.log('승률 result : ', result);
        return result.data;
      }
      throw new Error(result.error.message);
    },
    staleTime: 3 * 60 * 1000,
    enabled,
  });
};

// 특화된 상세 승률 훅들
export const useHomeAwayStats = (userId?: number, season?: Season) => {
  return useDetailedStats<{ homeStats: any; awayStats: any }>('homeAway', userId, season);
};

export const useStadiumStats = (userId?: number, season?: Season) => {
  return useDetailedStats<{ stadiumStats: Record<string, any> }>('stadium', userId, season);
};

export const useOpponentStats = (userId?: number, season?: Season) => {
  return useDetailedStats<{ opponentStats: Record<string, any> }>('opponent', userId, season);
};

export const useDayOfWeekStats = (userId?: number, season?: Season) => {
  return useDetailedStats<{ dayOfWeekStats: Record<string, any> }>('dayOfWeek', userId, season);
};

export const useStreakStats = (userId?: number, season?: Season) => {
  return useDetailedStats<{
    userId: number;
    season: string;
    wins: number;
    draws: number;
    losses: number;
    totalGames: number;
    maxWinStreakCurrentSeason: number;
    currentSeason: string;
    maxWinStreakBySeason: Record<string, number>;
    maxWinStreakAll: number;
    currentWinStreak: number;
  }>('streak', userId, season);
};

// 모든 통계 조합 훅
export const useAllUserStats = (userId?: number, season?: Season) => {
  const basicStats = useBasicStats(userId, season);
  const homeAwayStats = useHomeAwayStats(userId, season);
  const stadiumStats = useStadiumStats(userId, season);
  const opponentStats = useOpponentStats(userId, season);
  const dayOfWeekStats = useDayOfWeekStats(userId, season);
  const streakStats = useStreakStats(userId, season);
  const badges = useUserBadges(userId, season);

  const queryClient = useQueryClient();

  return {
    basicStats,
    homeAwayStats,
    stadiumStats,
    opponentStats,
    dayOfWeekStats,
    streakStats,
    badges,
    isLoading: basicStats.isLoading || homeAwayStats.isLoading || badges.isLoading,
    isError: basicStats.isError || homeAwayStats.isError || badges.isError,
    refetchAll: () => {
      return Promise.all([
        basicStats.refetch(),
        homeAwayStats.refetch(),
        stadiumStats.refetch(),
        opponentStats.refetch(),
        dayOfWeekStats.refetch(),
        streakStats.refetch(),
        badges.refetch(),
      ]);
    },
    invalidateAll: () => {
      return QueryInvalidator.invalidateEntity(queryClient, PROFILE_ENTITY);
    },
  };
};
