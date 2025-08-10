import React from 'react';
import { View, Text } from 'react-native';
import { StadiumStats } from '../../../../features/user-stats/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { styles } from './StadiumMapStats.style';

interface StadiumMapStatsProps {
  stadiumStats: StadiumStats[];
}

export const StadiumMapStats: React.FC<StadiumMapStatsProps> = ({ stadiumStats }) => {
  const themeColor = useThemeColor();

  const getStadiumColor = (winRate: number) => {
    if (winRate >= 70) return '#4CAF50';
    if (winRate >= 50) return '#FFC107';
    return '#F44336';
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>구장별 승률</Text>

      <View style={styles.mapContainer}>
        <View style={styles.mapPlaceholder}>
          {stadiumStats.map((stadium, index) => (
            <View
              key={stadium.stadiumId}
              style={[
                styles.stadiumMarker,
                {
                  backgroundColor: getStadiumColor(stadium.winRate),
                  left: `${(index + 1) * (100 / (stadiumStats.length + 1))}%`,
                  top: `${40 + (index % 3) * 20}%`,
                },
              ]}
            >
              <Text style={styles.markerText}>{stadium.winRate}%</Text>
            </View>
          ))}
        </View>
      </View>

      <View style={styles.stadiumList}>
        {stadiumStats.map((stadium) => (
          <View key={stadium.stadiumId} style={styles.stadiumItem}>
            <View style={styles.stadiumInfo}>
              <View style={[styles.stadiumIndicator, { backgroundColor: getStadiumColor(stadium.winRate) }]} />
              <Text style={styles.stadiumName}>{stadium.stadiumName}</Text>
            </View>
            <Text style={styles.stadiumWinRate}>{stadium.winRate}%</Text>
          </View>
        ))}
      </View>
    </View>
  );
};
