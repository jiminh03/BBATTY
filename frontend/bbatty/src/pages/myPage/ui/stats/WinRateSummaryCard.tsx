import React from 'react';
import { View, Text } from 'react-native';
import { BasicStats, HomeAwayStats, StreakStats } from '../../../../features/user-profile/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { Format } from '../../../../shared';
import { WinRateStatsHeader } from './WinRateStatsHeader';
import { styles } from './WinRateSummaryCard.style';

interface WinRateSummaryCardProps {
  basicStats: BasicStats;
  homeAwayStats?: HomeAwayStats;
  streakStats?: StreakStats;
}

interface CircularProgressProps {
  percentage: number;
  size: number;
  color: string;
}

const CircularProgress: React.FC<CircularProgressProps> = ({
  percentage,
  size,
  color
}) => {
  const strokeWidth = 8;
  
  return (
    <View style={{ width: size, height: size, position: 'relative' }}>
      {/* Background circle */}
      <View style={[
        styles.circleBackground,
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          borderWidth: strokeWidth,
          borderColor: '#E0E0E0',
        }
      ]} />
      
      {/* Progress overlay - simple approach */}
      {percentage > 0 && (
        <View 
          style={[
            styles.circleProgress,
            {
              width: size - strokeWidth,
              height: size - strokeWidth,
              borderRadius: (size - strokeWidth) / 2,
              borderWidth: strokeWidth / 2,
              borderTopColor: color,
              borderRightColor: percentage > 25 ? color : 'transparent',
              borderBottomColor: percentage > 50 ? color : 'transparent',
              borderLeftColor: percentage > 75 ? color : 'transparent',
              position: 'absolute',
              top: strokeWidth / 2,
              left: strokeWidth / 2,
              transform: [{ rotate: '-90deg' }],
            }
          ]}
        />
      )}
      
      <View style={[styles.progressCenter, { width: size, height: size }]}>
        <Text style={[styles.progressPercentage, { color }]}>{percentage}%</Text>
      </View>
    </View>
  );
};

export const WinRateSummaryCard: React.FC<WinRateSummaryCardProps> = ({ basicStats, homeAwayStats, streakStats }) => {
  const themeColor = useThemeColor();
  const overallWinRate = Format.winRate.toPercent(basicStats.winRate);
  const homeWinRate = homeAwayStats?.homeStats.winRate ? Format.winRate.toPercent(homeAwayStats.homeStats.winRate) : 0;
  const awayWinRate = homeAwayStats?.awayStats.winRate ? Format.winRate.toPercent(homeAwayStats.awayStats.winRate) : 0;

  return (
    <View style={styles.container}>
      <WinRateStatsHeader
        totalGames={basicStats.totalGames || 0}
        wins={basicStats.wins || 0}
        draws={basicStats.draws || 0}
        losses={basicStats.losses || 0}
        streakStats={streakStats}
      />

      {homeAwayStats && (
        <View style={styles.progressCard}>
          <View style={styles.progressContainer}>
            <View style={styles.progressItem}>
              <CircularProgress
                percentage={homeWinRate}
                size={120}
                color={themeColor}
              />
              <Text style={styles.progressLabel}>홈</Text>
            </View>

            <View style={styles.progressItem}>
              <CircularProgress
                percentage={awayWinRate}
                size={120}
                color={themeColor}
              />
              <Text style={styles.progressLabel}>원정</Text>
            </View>
          </View>
        </View>
      )}
    </View>
  );
};
