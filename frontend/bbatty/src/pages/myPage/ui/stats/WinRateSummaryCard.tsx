import React from 'react';
import { View, Text } from 'react-native';
import { BasicStats, HomeAwayStats } from '../../../../features/user-profile/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { Format } from '../../../../shared';
import { styles } from './WinRateSummaryCard.style';

interface WinRateSummaryCardProps {
  basicStats: BasicStats;
  homeAwayStats?: HomeAwayStats;
}

export const WinRateSummaryCard: React.FC<WinRateSummaryCardProps> = ({ basicStats, homeAwayStats }) => {
  const themeColor = useThemeColor();

  return (
    <View style={styles.container}>
      <Text style={styles.title}>승률</Text>

      <View style={styles.mainWinRate}>
        <Text style={[styles.overallRate, { color: themeColor }]}>{Format.winRate.toPercent(basicStats.winRate)}%</Text>
        <Text style={styles.overallLabel}>전체</Text>
      </View>

      {homeAwayStats && (
        <View style={styles.detailRates}>
          <View style={styles.rateItem}>
            <Text style={[styles.rateNumber, { color: themeColor }]}>
              {homeAwayStats.homeStats.winRate ? Format.winRate.toPercent(homeAwayStats.homeStats.winRate) : 0}%
            </Text>
            <Text style={styles.rateLabel}>홈</Text>
          </View>

          <View style={styles.rateDivider} />

          <View style={styles.rateItem}>
            <Text style={[styles.rateNumber, { color: themeColor }]}>
              {homeAwayStats.awayStats.winRate ? Format.winRate.toPercent(homeAwayStats.awayStats.winRate) : 0}%
            </Text>
            <Text style={styles.rateLabel}>원정</Text>
          </View>
        </View>
      )}

      {/* <View style={styles.statsRow}>
        <View style={styles.statItem}>
          <Text style={styles.statNumber}>{Format.game.count(basicStats.totalGames)}</Text>
          <Text style={styles.statLabel}>총 경기</Text>
        </View>
        <View style={styles.statItem}>
          <Text style={[styles.statNumber, { color: '#4CAF50' }]}>{Format.game.count(basicStats.wins)}</Text>
          <Text style={styles.statLabel}>승</Text>
        </View>
        <View style={styles.statItem}>
          <Text style={[styles.statNumber, { color: '#F44336' }]}>{Format.game.count(basicStats.losses)}</Text>
          <Text style={styles.statLabel}>패</Text>
        </View>
        {basicStats.draws > 0 && (
          <View style={styles.statItem}>
            <Text style={[styles.statNumber, { color: '#FFC107' }]}>{Format.game.count(basicStats.draws)}</Text>
            <Text style={styles.statLabel}>무</Text>
          </View>
        )} */}
      {/* </View> */}
    </View>
  );
};
