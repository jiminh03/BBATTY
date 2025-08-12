import React from 'react';
import { View, Text, ScrollView } from 'react-native';
import { TeamStats, StreakStats } from '../../../../features/user-profile/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
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
  streakStats
}) => {
  const themeColor = useThemeColor();

  const maxWinRate = 100; // 최대값을 100%로 고정

  // 팀별 고유 색상 매핑
  const getTeamColor = (teamName: string) => {
    const teamColors: Record<string, string> = {
      'KIA': '#EA0029',
      '한화': '#FF6600', 
      'SSG': '#CE0E2D',
      '삼성': '#074CA1',
      '키움': '#820024',
      'LG': '#C30452',
      'KT': '#000000',
      '기아': '#ED1C24',
      '로데': '#002955',
      'NC': '#315288',
    };
    return teamColors[teamName] || themeColor;
  };

  return (
    <View style={styles.container}>
      <WinRateStatsHeader
        totalGames={totalGames}
        wins={wins}
        draws={draws}
        losses={losses}
        streakStats={streakStats}
      />

      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={styles.chartContainer}>
          {teamStats.map((team, index) => {
            const teamColor = getTeamColor(team.teamName);
            const barHeight = (team.winRate / maxWinRate) * 120;
            const is100Percent = team.winRate === 100;
            
            return (
              <View key={team.teamId} style={styles.barColumn}>
                <Text style={[styles.winRateLabel, { color: teamColor }]}>
                  {team.winRate}%
                </Text>
                <View style={styles.barContainer}>
                  <View
                    style={[
                      styles.bar,
                      {
                        height: barHeight,
                        backgroundColor: teamColor,
                      },
                      is100Percent && styles.perfectBar
                    ]}
                  />
                </View>
                <Text style={styles.teamName}>{team.teamName}</Text>
              </View>
            );
          })}
        </View>
      </ScrollView>
    </View>
  );
};
