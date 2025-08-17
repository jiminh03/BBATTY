import React from 'react';
import { View, Text, ScrollView } from 'react-native';
import { TeamStats, StreakStats } from '../../../../features/user-profile/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { useUserStore } from '../../../../entities/user/model/userStore';
import { TEAMS, getTeamInfo } from '../../../../shared/team/teamTypes';
import { WinRateStatsHeader } from './WinRateStatsHeader';
import { styles } from './TeamStatsChart.style';

interface TeamStatsChartProps {
  teamStats: TeamStats[];
  totalGames?: number;
  winRate?: string;
  wins?: number;
  draws?: number;
  losses?: number;
  streakStats?: StreakStats;
}

export const TeamStatsChart: React.FC<TeamStatsChartProps> = ({
  teamStats,
  totalGames = 0,
  winRate = '0',
  wins = 0,
  draws = 0,
  losses = 0,
  streakStats,
}) => {
  const themeColor = useThemeColor();
  const currentUser = useUserStore((s) => s.currentUser);
  const userTeamId = currentUser?.teamId;

  const maxWinRate = 100; // 최대값을 100%로 고정

  // 팀 이름에서 핵심 이름만 추출 (띄어쓰기 앞의 부분)
  const getShortTeamName = (fullTeamName: string): string => {
    return fullTeamName.split(' ')[0];
  };

  // 모든 KBO 팀들을 ID 순서대로 정렬하고 승률 데이터 매핑 (본인 팀 제외)
  const getAllTeamsWithStats = () => {
    return TEAMS.filter((team) => team.id !== userTeamId) // 본인 팀 제외
      .map((team) => {
        // teamStats에서 해당 팀의 데이터 찾기
        const teamData = teamStats.find((stat) => stat.teamId === team.id);

        return {
          teamId: team.id,
          teamName: getShortTeamName(team.name),
          matches: teamData?.matches || 0,
          wins: teamData?.wins || 0,
          draws: teamData?.draws || 0,
          losses: teamData?.losses || 0,
          winRate: teamData?.winRate || 0,
          color: team.color,
          hasData: !!teamData && teamData.matches > 0,
        };
      });
  };

  const allTeamsData = getAllTeamsWithStats();

  return (
    <View style={styles.container}>
      <WinRateStatsHeader totalGames={totalGames} wins={wins} draws={draws} losses={losses} streakStats={streakStats} />

      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={styles.chartContainer}>
          {allTeamsData.map((team, index) => {
            const barHeight = team.hasData ? (team.winRate / maxWinRate) * 120 : 0;
            const is100Percent = team.winRate === 100;

            return (
              <View key={team.teamId} style={styles.barColumn}>
                <Text style={[styles.winRateLabel, { color: team.color }]}>
                  {team.hasData ? `${team.winRate}%` : '--%'}
                </Text>
                <View style={styles.barContainer}>
                  <View
                    style={[
                      styles.bar,
                      {
                        height: barHeight,
                        backgroundColor: team.color,
                      },
                      is100Percent && styles.perfectBar,
                    ]}
                  />
                </View>
                <Text style={styles.teamName}>{team.teamName}</Text>
                <Text style={styles.recordText}>
                  {team.hasData ? `(${team.wins}/${team.draws}/${team.losses})` : '(0/0/0)'}
                </Text>
              </View>
            );
          })}
        </View>
      </ScrollView>
    </View>
  );
};
