import React from 'react';
import { View, Text } from 'react-native';
import { DayOfWeekStats } from '../../../../features/user-stats/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { styles } from './DailyStatsGrid.style';

interface DailyStatsGridProps {
  dayStats: DayOfWeekStats[];
  totalStats: {
    overall: number;
    home: number;
    away: number;
  };
}
export const DailyStatsGrid: React.FC<DailyStatsGridProps> = ({ dayStats }) => {
  const themeColor = useThemeColor();

  // 월요일 제외한 6개 요일 (화~일)
  const weekdays = ['화', '수', '목', '금', '토', '일'];

  const topRowDays = weekdays.slice(0, 3); // 화, 수, 목
  const bottomRowDays = weekdays.slice(3); // 금, 토, 일

  const getDayStats = (dayName: string) => {
    return dayStats.find((d) => d.dayName === dayName);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>요일별 승률</Text>

      <View style={styles.grid}>
        {/* 상단 행: 화수목 */}
        <View style={styles.row}>
          {topRowDays.map((day) => {
            const stats = getDayStats(day);
            return (
              <View key={day} style={styles.statCard}>
                <Text style={[styles.statValue, { color: themeColor }]}>{stats?.winRate || 0}%</Text>
                <Text style={styles.statLabel}>{day}요일</Text>
              </View>
            );
          })}
        </View>

        {/* 하단 행: 금토일 */}
        <View style={styles.row}>
          {bottomRowDays.map((day) => {
            const stats = getDayStats(day);
            return (
              <View key={day} style={styles.statCard}>
                <Text style={[styles.statValue, { color: themeColor }]}>{stats?.winRate || 0}%</Text>
                <Text style={styles.statLabel}>{day}요일</Text>
              </View>
            );
          })}
        </View>
      </View>
    </View>
  );
};
