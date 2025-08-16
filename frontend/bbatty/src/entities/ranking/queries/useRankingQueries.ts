import { useQuery, UseQueryResult } from '@tanstack/react-query';
import { apiClient } from '../../../shared/api/client/apiClient';
import { QueryKeys } from '../../../shared/api/lib/tanstack/queryKeyTypes';

export interface UserRanking {
  userId: number;
  nickname: string;
  winRate: number;
  rank: number;
  percentile: number | null;
  isCurrentUser: boolean;
  userTeamId?: number;
}

export interface RankingResponse {
  season: string;
  rankings: UserRanking[];
  myRanking: UserRanking | null;
}

export interface TeamRankingResponse {
  teamId: number;
  teamName: string;
  season: string;
  rankings: UserRanking[];
  myRanking: UserRanking | null;
}

// 전체 랭킹 조회
export const useGlobalRanking = (): UseQueryResult<RankingResponse, Error> => {
  return useQuery({
    queryKey: QueryKeys.entity('ranking').concat(['global']),
    queryFn: async () => {
      const response = await apiClient.get<{ status: string; message: string; data: RankingResponse }>(
        `/api/ranking/global`
      );
      
      if (response.data.status !== 'SUCCESS') {
        throw new Error(response.data.message || '전체 랭킹 조회 실패');
      }
      
      return response.data.data;
    },
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분 (이전 cacheTime)
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });
};

// 팀별 랭킹 조회
export const useTeamRanking = (teamId: number): UseQueryResult<TeamRankingResponse, Error> => {
  return useQuery({
    queryKey: QueryKeys.entity('ranking').concat(['team', teamId]),
    queryFn: async () => {
      const response = await apiClient.get<{ status: string; message: string; data: TeamRankingResponse }>(
        `/api/ranking/team/${teamId}`
      );
      
      if (response.data.status !== 'SUCCESS') {
        throw new Error(response.data.message || '팀 랭킹 조회 실패');
      }
      
      return response.data.data;
    },
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
    retry: 3,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    enabled: !!teamId, // teamId가 있을 때만 실행
  });
};

// 모든 팀 랭킹을 한번에 조회하는 훅
export const useAllTeamRankings = (teamIds: number[]) => {
  return useQuery({
    queryKey: QueryKeys.entity('ranking').concat(['all-teams', teamIds.sort()]),
    queryFn: async () => {
      const promises = teamIds.map(async (teamId) => {
        try {
          const response = await apiClient.get<{ status: string; message: string; data: TeamRankingResponse }>(
            `/api/ranking/team/${teamId}`
          );
          
          if (response.data.status === 'SUCCESS') {
            return {
              teamId,
              teamName: response.data.data.teamName,
              rankings: response.data.data.rankings,
              myRanking: response.data.data.myRanking
            };
          }
        } catch (error) {
          console.error(`팀 ${teamId} 랭킹 조회 실패:`, error);
        }
        return null;
      });

      const results = await Promise.all(promises);
      
      // 성공한 결과만 필터링
      const successResults = results.filter(result => result !== null);
      
      // 팀별 데이터 구조로 변환
      const teamRankingsData: {[key: string]: UserRanking[]} = {};
      const myTeamRankings: {[key: string]: UserRanking | null} = {};
      
      successResults.forEach((result) => {
        if (result) {
          teamRankingsData[result.teamName] = result.rankings;
          myTeamRankings[result.teamName] = result.myRanking;
        }
      });
      
      return {
        teamRankingsData,
        myTeamRankings
      };
    },
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
    retry: 2,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    enabled: teamIds.length > 0,
  });
};