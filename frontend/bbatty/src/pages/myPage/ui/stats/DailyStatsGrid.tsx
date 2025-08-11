import React from 'react';
import { View, Text } from 'react-native';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { Format } from '../../../../shared';
import { styles } from './DailyStatsGrid.style';

interface DayStats {
  dayName: string;
  matches: number;
  wins: number;
  winRate: number;
}

interface DailyStatsGrid {
  dayStats: DayStats[];
}

export const DetailedStatsGrid: React.FC<DailyStatsGrid> = ({ dayStats }) => {
  const themeColor = useThemeColor();

  const weekdays = ['화', '수', '목', '금', '토', '일'];
  const topRowDays = weekdays.slice(0, 3);
  const bottomRowDays = weekdays.slice(3);

  const getDayStats = (dayName: string) => {
    return dayStats.find((d) => d.dayName === dayName);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>요일별 승률</Text>

      <View style={styles.grid}>
        <View style={styles.row}>
          {topRowDays.map((day) => {
            const stats = getDayStats(day);
            const winRate = stats ? Format.percent.basic(stats.wins, stats.matches) : 0;

            return (
              <View key={day} style={styles.statCard}>
                <Text style={[styles.statValue, { color: themeColor }]}>{winRate}%</Text>
                <Text style={styles.statLabel}>{day}요일</Text>
              </View>
            );
          })}
        </View>

        <View style={styles.row}>
          {bottomRowDays.map((day) => {
            const stats = getDayStats(day);
            const winRate = stats ? Format.percent.basic(stats.wins, stats.matches) : 0;

            return (
              <View key={day} style={styles.statCard}>
                <Text style={[styles.statValue, { color: themeColor }]}>{winRate}%</Text>
                <Text style={styles.statLabel}>{day}요일</Text>
              </View>
            );
          })}
        </View>
      </View>
    </View>
  );
};
