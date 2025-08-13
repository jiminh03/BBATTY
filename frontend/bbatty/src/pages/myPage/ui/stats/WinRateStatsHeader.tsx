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
  return (
    <View style={styles.container}>
      <View style={styles.statsRow}>
        <Text style={styles.statItem}>총 {totalGames}경기</Text>
      </View>
      <View style={[styles.statsRow, styles.recordRow]}>
        <Text style={styles.recordText}>{wins}승 {draws}무 {losses}패</Text>
      </View>
      <View style={styles.statsRow}>
        <Text style={styles.statItem}>현재 연승: {streakStats?.currentWinStreak || 0}</Text>
      </View>
      <View style={styles.statsRow}>
        <Text style={styles.statItem}>최고 연승: {streakStats?.maxWinStreakAll || 0}</Text>
      </View>
    </View>
  );
};