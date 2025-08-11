import React from 'react';
import { View, Text, ScrollView } from 'react-native';
import { TeamStats } from '../../../../features/user-stats/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { styles } from './TeamStatsChart.style';

interface TeamStatsChartProps {
  teamStats: TeamStats[];
}

export const TeamStatsChart: React.FC<TeamStatsChartProps> = ({ teamStats }) => {
  const themeColor = useThemeColor();

  const maxWinRate = Math.max(...teamStats.map((team) => team.winRate));

  return (
    <View style={styles.container}>
      <Text style={styles.title}>상대팀별 승률</Text>

      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={styles.chartContainer}>
          {teamStats.map((team, index) => (
            <View key={team.teamId} style={styles.barColumn}>
              <View style={styles.barContainer}>
                <View
                  style={[
                    styles.bar,
                    {
                      height: (team.winRate / maxWinRate) * 120,
                      backgroundColor: themeColor,
                    },
                  ]}
                />
              </View>
              <Text style={styles.teamName}>{team.teamName}</Text>
              <Text style={styles.winRate}>{team.winRate}%</Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </View>
  );
};
