import { useQuery } from '@tanstack/react-query';
import { apiClient } from '../../../shared/api/client/apiClient';
import { TEAMS, Team } from '../../../shared/team/teamTypes';

export type TeamStanding = {
  rank: number;
  wins: number;
  draws: number;
  losses: number;
  winRate: number;     // 0.625 처럼 숫자
  teamName: string;
};

type RankingResponse = {
  status: 'SUCCESS' | 'ERROR';
  data?: Array<{
    teamName: string;
    games: number;
    wins: number;
    draws: number;
    losses: number;
    winRate: number;   // API가 0.625 숫자 형태라면 그대로 사용
    gameBehind: number;
    streak: number;
    streakText: string;
  }>;
};

async function fetchRankings(): Promise<TeamStanding[]> {
  const res = await apiClient.get('/api/teams/ranking');
  const root = (res as any)?.data ?? res;
  const payload: RankingResponse = root;

  if (payload?.status !== 'SUCCESS' || !Array.isArray(payload?.data)) {
    throw new Error('순위 데이터를 불러오지 못했습니다.');
  }

  // API에는 rank 필드가 없고 index 기반 순위이므로 index+1로 계산
  return payload.data.map((item, idx) => ({
    rank: idx + 1,
    teamName: item.teamName,
    wins: item.wins,
    draws: item.draws,
    losses: item.losses,
    winRate: item.winRate,
  }));
}

/** 팀ID로 해당 팀의 순위/전적만 선택해서 반환 */
export function useTeamStanding(teamId: number) {
  return useQuery({
    queryKey: ['teamRankingList'],      // 리스트를 캐싱
    queryFn: fetchRankings,
    select: (list): TeamStanding | undefined => {
      const team: Team | undefined = TEAMS.find(t => t.id === teamId);
      if (!team) return undefined;
      return list.find(r => r.teamName === team.name);
    },
    staleTime: 60_000,
    enabled: !!teamId,
  });
}
