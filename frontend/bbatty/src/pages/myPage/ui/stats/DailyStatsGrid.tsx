import React from 'react';
import { View, Text, ScrollView } from 'react-native';
import { DayOfWeekStatsItem, StreakStats } from '../../../../features/user-profile/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { WinRateStatsHeader } from './WinRateStatsHeader';
import { styles } from './DailyStatsGrid.style';

interface DayStatsWithRecord extends DayOfWeekStatsItem {
  draws: number;
  losses: number;
}

interface DailyStatsGridProps {
  dayStats: DayStatsWithRecord[];
  totalGames?: number;
  winRate?: string;
  wins?: number;
  draws?: number;
  losses?: number;
  streakStats?: StreakStats;
}

export const DetailedStatsGrid: React.FC<DailyStatsGridProps> = ({
  dayStats,
  totalGames = 0,
  winRate = '0',
  wins = 0,
  draws = 0,
  losses = 0,
  streakStats,
}) => {
  const themeColor = useThemeColor();

  // 영어 요일명을 한글로 매핑
  const dayMapping: Record<string, string> = {
    'TUESDAY': '화',
    'WEDNESDAY': '수',
    'THURSDAY': '목',
    'FRIDAY': '금',
    'SATURDAY': '토',
    'SUNDAY': '일',
  };

  // 요일 순서 정의 (화수목금토일)
  const weekdays = ['TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
  const topRowDays = weekdays.slice(0, 3); // 화, 수, 목
  const bottomRowDays = weekdays.slice(3); // 금, 토, 일

  const getDayStats = (dayKey: string) => {
    const dayData = dayStats.find((stat) => stat.dayName === dayKey);
    return {
      dayName: dayMapping[dayKey],
      matches: dayData?.matches || 0,
      wins: dayData?.wins || 0,
      draws: dayData?.draws || 0,
      losses: dayData?.losses || 0,
      winRate: dayData?.winRate || 0,
    };
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

      <View style={styles.calendarGrid}>
        {/* 첫 번째 행: 화, 수, 목 */}
        <View style={styles.calendarRow}>
          {topRowDays.map((dayKey) => {
            const stats = getDayStats(dayKey);
            
            return (
              <View key={dayKey} style={styles.calendarCell}>
                <View style={[styles.calendarHeader, { backgroundColor: themeColor }]}>
                  <Text style={styles.dayLabel}>{stats.dayName}</Text>
                </View>
                <View style={styles.calendarBody}>
                  <Text style={[styles.winRate, { color: themeColor }]}>{stats.winRate}%</Text>
                  <Text style={styles.recordText}>
                    ({stats.wins}/{stats.draws}/{stats.losses})
                  </Text>
                </View>
              </View>
            );
          })}
        </View>

        {/* 두 번째 행: 금, 토, 일 */}
        <View style={[styles.calendarRow, styles.lastRow]}>
          {bottomRowDays.map((dayKey) => {
            const stats = getDayStats(dayKey);
            
            return (
              <View key={dayKey} style={styles.calendarCell}>
                <View style={[styles.calendarHeader, { backgroundColor: themeColor }]}>
                  <Text style={styles.dayLabel}>{stats.dayName}</Text>
                </View>
                <View style={styles.calendarBody}>
                  <Text style={[styles.winRate, { color: themeColor }]}>{stats.winRate}%</Text>
                  <Text style={styles.recordText}>
                    ({stats.wins}/{stats.draws}/{stats.losses})
                  </Text>
                </View>
              </View>
            );
          })}
        </View>
      </View>
    </View>
  );
};