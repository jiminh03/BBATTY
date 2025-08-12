import React from 'react';
import { View, Text } from 'react-native';
import { StreakStats } from '../../../../features/user-profile/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { Format } from '../../../../shared';
import { WinRateStatsHeader } from './WinRateStatsHeader';
import { styles } from './DailyStatsGrid.style';

interface DayStats {
  dayName: string;
  matches: number;
  wins: number;
  winRate: number;
}

interface DailyStatsGrid {
  dayStats: DayStats[];
  totalGames?: number;
  winRate?: string;
  wins?: number;
  draws?: number;
  losses?: number;
  streakStats?: StreakStats;
}

export const DetailedStatsGrid: React.FC<DailyStatsGrid> = ({ 
  dayStats,
  totalGames = 0,
  winRate = '0',
  wins = 0,
  draws = 0,
  losses = 0,
  streakStats
}) => {
  const themeColor = useThemeColor();

  // API 응답에서 올 수 있는 영어 요일명을 한국어로 매핑
  const dayMapping: Record<string, string> = {
    'MONDAY': '월',
    'TUESDAY': '화', 
    'WEDNESDAY': '수',
    'THURSDAY': '목',
    'FRIDAY': '금',
    'SATURDAY': '토',
    'SUNDAY': '일',
    // 한국어도 지원
    '월': '월', '화': '화', '수': '수', '목': '목', '금': '금', '토': '토', '일': '일'
  };

  const weekdays = ['월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];
  const topRowDays = weekdays.slice(0, 3);
  const bottomRowDays = weekdays.slice(3);

  const getDayStats = (dayLabel: string) => {
    const dayKey = dayLabel.replace('요일', '');
    return dayStats.find((d) => {
      const mappedDay = dayMapping[d.dayName] || d.dayName;
      return mappedDay === dayKey;
    });
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

      <View style={styles.grid}>
        <View style={styles.row}>
          {topRowDays.map((dayLabel) => {
            const stats = getDayStats(dayLabel);
            const winRate = stats ? Format.percent.basic(stats.wins, stats.matches) : 0;

            return (
              <View key={dayLabel} style={styles.statCard}>
                <Text style={[styles.statValue, { color: themeColor }]}>{winRate}%</Text>
                <Text style={styles.statLabel}>{dayLabel}</Text>
              </View>
            );
          })}
        </View>

        <View style={styles.row}>
          {bottomRowDays.map((dayLabel) => {
            const stats = getDayStats(dayLabel);
            const winRate = stats ? Format.percent.basic(stats.wins, stats.matches) : 0;

            return (
              <View key={dayLabel} style={styles.statCard}>
                <Text style={[styles.statValue, { color: themeColor }]}>{winRate}%</Text>
                <Text style={styles.statLabel}>{dayLabel}</Text>
              </View>
            );
          })}
        </View>
      </View>
    </View>
  );
};
