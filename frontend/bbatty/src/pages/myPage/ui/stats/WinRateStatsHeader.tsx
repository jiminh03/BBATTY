import React from 'react';
import { View, Text } from 'react-native';
import { StreakStats } from '../../../../features/user-profile/model/statsTypes';
import { Format } from '../../../../shared';
import { styles } from './WinRateStatsHeader.style';

interface WinRateStatsHeaderProps {
  totalGames: number;
  wins: number;
  draws: number;
  losses: number;
  streakStats?: StreakStats;
}

export const WinRateStatsHeader: React.FC<WinRateStatsHeaderProps> = ({
  totalGames = 0,
  wins = 0,
  draws = 0,
  losses = 0,
  streakStats,
}) => {
  // streakStats에서 실제 데이터 사용 (API 응답에 포함된 기본 통계)
  const actualTotalGames = streakStats?.totalGames || totalGames;
  const actualWins = streakStats?.wins || wins;
  const actualDraws = streakStats?.draws || draws;
  const actualLosses = streakStats?.losses || losses;
  
  return (
    <View style={styles.container}>
      <View style={styles.statsRow}>
        <Text style={styles.statItem}>총 {actualTotalGames}경기</Text>
      </View>
      <View style={[styles.statsRow, styles.recordRow]}>
        <Text style={styles.recordText}>{actualWins}승 {actualDraws}무 {actualLosses}패</Text>
      </View>
      <View style={styles.statsRow}>
        <Text style={styles.statItem}>현재 연승: {streakStats?.currentWinStreak || 0}</Text>
        <Text style={styles.statItem}>최고 연승: {streakStats?.maxWinStreakAll || 0}</Text>
      </View>
    </View>
  );
};