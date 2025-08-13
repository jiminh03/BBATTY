import React from 'react';
import { View, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { StadiumStatsItem, StreakStats } from '../../../../features/user-profile/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { WinRateStatsHeader } from './WinRateStatsHeader';
import { styles } from './StadiumMapStats.style';

interface StadiumMapStatsProps {
  stadiumStats: StadiumStatsItem[];
  totalGames?: number;
  winRate?: string;
  wins?: number;
  draws?: number;
  losses?: number;
  streakStats?: StreakStats;
}

export const StadiumMapStats: React.FC<StadiumMapStatsProps> = ({ 
  stadiumStats,
  totalGames = 0,
  winRate = '0',
  wins = 0,
  draws = 0,
  losses = 0,
  streakStats
}) => {
  const themeColor = useThemeColor();

  // 실제 구장 좌표 (대략적인 위치)
  const stadiumPositions: Record<string, { left: string; top: string }> = {
    '수원KT위즈파크': { left: '40%', top: '45%' },
    '인천SSG랜더스필드': { left: '38%', top: '40%' },
    '대전한화생명볼파크': { left: '50%', top: '60%' },
    '광주KIA챔피언스필드': { left: '35%', top: '80%' },
    '대구삼성라이온즈파크': { left: '70%', top: '70%' },
    '부산사직야구장': { left: '75%', top: '85%' },
    '창원NC파크': { left: '65%', top: '75%' },
    '잠실야구장': { left: '45%', top: '35%' },
    '고척스카이돔': { left: '42%', top: '38%' },
  };

  const getWinRateColor = (winRate: number) => {
    if (winRate >= 80) return '#4CAF50'; // 초록
    if (winRate >= 60) return '#2196F3'; // 파란
    if (winRate >= 40) return '#FF9800'; // 주황
    return '#F44336'; // 빨간
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

      {/* 한국 지도 배경 */}
      <View style={styles.mapContainer}>
        <View style={styles.koreaMap}>
          {/* 지도 배경 이미지 자리 */}
          <View style={styles.mapBackground} />
          
          {/* 구장 마커 */}
          {stadiumStats.map((stadium) => {
            const position = stadiumPositions[stadium.stadiumName] || { left: '50%', top: '50%' };
            const color = getWinRateColor(stadium.winRate);
            
            return (
              <View
                key={stadium.stadiumId}
                style={[
                  styles.stadiumMarker,
                  {
                    left: position.left,
                    top: position.top,
                    backgroundColor: color,
                  },
                ]}
              >
                <Ionicons name="location" size={24} color="white" />
                <View style={styles.stadiumInfo}>
                  <Text style={styles.stadiumLabel}>{stadium.stadiumName}</Text>
                  <Text style={styles.stadiumWinRate}>{stadium.winRate}% ({stadium.wins}승 {stadium.matches}경기)</Text>
                </View>
              </View>
            );
          })}
        </View>
      </View>
    </View>
  );
};
