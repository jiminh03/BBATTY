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

const CircularProgress: React.FC<CircularProgressProps> = ({ percentage, size, color }) => {
  const strokeWidth = 8;
  
  // Create an array of small segments to approximate the circle
  const segments = 100; // Number of segments for smooth circle
  const segmentAngle = 360 / segments;
  const progressSegments = Math.round((percentage / 100) * segments);

  return (
    <View style={{ width: size, height: size, position: 'relative' }}>
      {/* Background circle */}
      <View
        style={[
          styles.circleBackground,
          {
            width: size,
            height: size,
            borderRadius: size / 2,
            borderWidth: strokeWidth,
            borderColor: '#E0E0E0',
          },
        ]}
      />

      {/* Progress segments */}
      {percentage > 0 && (
        <View
          style={{
            position: 'absolute',
            width: size,
            height: size,
          }}
        >
          {Array.from({ length: progressSegments }, (_, index) => {
            // Start from 12 o'clock (0deg) and go counterclockwise
            const rotation = -(index * segmentAngle); // Negative for counterclockwise
            
            return (
              <View
                key={index}
                style={{
                  position: 'absolute',
                  width: size,
                  height: size,
                  transform: [{ rotate: `${rotation}deg` }],
                }}
              >
                <View
                  style={{
                    width: strokeWidth + 2, // Make it slightly wider to cover background
                    height: strokeWidth,
                    backgroundColor: color,
                    position: 'absolute',
                    top: -strokeWidth / 2, // Position to cover the border
                    left: size / 2 - (strokeWidth + 2) / 2,
                    borderRadius: strokeWidth / 2,
                  }}
                />
              </View>
            );
          })}
        </View>
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
              <CircularProgress percentage={homeWinRate} size={120} color={themeColor} />
              <Text style={styles.progressLabel}>홈</Text>
              <Text style={styles.recordText}>
                {homeAwayStats.homeStats.wins}승 {homeAwayStats.homeStats.draws}무 {homeAwayStats.homeStats.losses}패
              </Text>
            </View>

            <View style={styles.progressItem}>
              <CircularProgress percentage={awayWinRate} size={120} color={themeColor} />
              <Text style={styles.progressLabel}>원정</Text>
              <Text style={styles.recordText}>
                {homeAwayStats.awayStats.wins}승 {homeAwayStats.awayStats.draws}무 {homeAwayStats.awayStats.losses}패
              </Text>
            </View>
          </View>
        </View>
      )}
    </View>
  );
};
