import React from 'react';
import { View, Text } from 'react-native';
import { WinRateStats } from '../../../../features/user-stats/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { styles } from './WinRateSummaryCard.style';

interface WinRateSummaryCardProps {
  winRates: WinRateStats;
}

export const WinRateSummaryCard: React.FC<WinRateSummaryCardProps> = ({ winRates }) => {
  const themeColor = useThemeColor();

  return (
    <View style={styles.container}>
      <Text style={styles.title}>승률</Text>

      <View style={styles.mainWinRate}>
        <Text style={[styles.overallRate, { color: themeColor }]}>{winRates.overall}%</Text>
        <Text style={styles.overallLabel}>전체</Text>
      </View>

      <View style={styles.detailRates}>
        <View style={styles.rateItem}>
          <Text style={[styles.rateNumber, { color: themeColor }]}>{winRates.home}%</Text>
          <Text style={styles.rateLabel}>홈</Text>
        </View>

        <View style={styles.rateDivider} />

        <View style={styles.rateItem}>
          <Text style={[styles.rateNumber, { color: themeColor }]}>{winRates.away}%</Text>
          <Text style={styles.rateLabel}>원정</Text>
        </View>
      </View>
    </View>
  );
};
