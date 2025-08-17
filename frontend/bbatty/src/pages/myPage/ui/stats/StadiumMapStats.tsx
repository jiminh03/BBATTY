import React from 'react';
import { View, Text, ScrollView } from 'react-native';
import { StadiumStatsItem, StreakStats } from '../../../../features/user-profile/model/statsTypes';
import { useThemeColor } from '../../../../shared/team/ThemeContext';
import { WinRateStatsHeader } from './WinRateStatsHeader';
import { styles } from './StadiumMapStats.style';

interface StadiumMapStatsProps {
  stadiumStats: Record<string, {
    games: number;
    wins: number;
    draws: number;
    losses: number;
    winRate: string;
  }>;
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

  // 디버깅용 로그
  console.log('🏟️ [StadiumMapStats] 받은 데이터:', stadiumStats);

  // 9개 고정 구장 정의
  const FIXED_STADIUMS = [
    '수원KT위즈파크',
    '인천SSG랜더스필드', 
    '대전한화생명볼파크',
    '광주KIA챔피언스필드',
    '대구삼성라이온즈파크',
    '부산사직야구장',
    '창원NC파크',
    '잠실야구장',
    '고척스카이돔'
  ];

  const getWinRateColor = (winRate: number) => {
    if (winRate >= 70) return '#4CAF50'; // 초록
    if (winRate >= 50) return '#2196F3'; // 파란
    if (winRate >= 30) return '#FF9800'; // 주황
    return '#F44336'; // 빨간
  };

  // 서버 데이터와 고정 구장 매칭
  const getStadiumData = () => {
    const stadiumData = FIXED_STADIUMS.map((stadiumName, index) => {
      // 서버에서 온 데이터에서 해당 구장 찾기
      const serverData = stadiumStats[stadiumName];
      
      console.log(`🏟️ [${stadiumName}] 서버 데이터:`, serverData);

      // 서버 데이터가 있으면 사용, 없으면 기본값 (0승 0무 0패 0%)
      const result = {
        stadiumId: index + 1,
        stadiumName,
        matches: serverData?.games || 0,
        wins: serverData?.wins || 0,
        draws: serverData?.draws || 0,
        losses: serverData?.losses || 0,
        winRate: serverData ? parseFloat(serverData.winRate) * 100 : 0,
      };
      
      console.log(`🏟️ [${stadiumName}] 변환된 데이터:`, result);
      return result;
    });

    // 데이터 무결성 검증
    const stadiumTotals = stadiumData.reduce((acc, stadium) => ({
      totalMatches: acc.totalMatches + stadium.matches,
      totalWins: acc.totalWins + stadium.wins,
      totalDraws: acc.totalDraws + stadium.draws,
      totalLosses: acc.totalLosses + stadium.losses,
    }), { totalMatches: 0, totalWins: 0, totalDraws: 0, totalLosses: 0 });

    console.log('🏟️ [데이터 검증] 구장별 합계:', stadiumTotals);
    console.log('🏟️ [데이터 검증] 전체 통계:', { 
      totalGames, 
      wins, 
      draws, 
      losses 
    });

    // 차이 계산
    const differences = {
      gamesDiff: totalGames - stadiumTotals.totalMatches,
      winsDiff: wins - stadiumTotals.totalWins,
      drawsDiff: draws - stadiumTotals.totalDraws,
      lossesDiff: losses - stadiumTotals.totalLosses,
    };

    console.log('🏟️ [데이터 검증] 차이:', differences);

    if (differences.gamesDiff !== 0 || differences.winsDiff !== 0 || differences.lossesDiff !== 0) {
      console.warn('⚠️ [데이터 불일치] 전체 통계와 구장별 합계가 다릅니다!');
    } else {
      console.log('✅ [데이터 일치] 전체 통계와 구장별 합계가 일치합니다.');
    }

    return stadiumData;
  };

  const renderStadiumCard = (stadium: any) => {
    const winRateNum = Number(stadium.winRate) || 0;
    const hasData = stadium.matches > 0; // 경기가 있는지 확인
    const winRateColor = hasData ? getWinRateColor(winRateNum) : '#9E9E9E'; // 데이터 없으면 회색

    return (
      <View key={stadium.stadiumId} style={styles.stadiumCard}>
        <View style={styles.cardHeader}>
          <View style={styles.stadiumInfo}>
            <Text style={[styles.stadiumName, !hasData && { color: '#9E9E9E' }]}>
              {stadium.stadiumName}
            </Text>
          </View>
          <View style={styles.winRateContainer}>
            <Text style={[styles.winRateText, { color: winRateColor }]}>
              {winRateNum.toFixed(1)}%
            </Text>
          </View>
        </View>

        {/* 승률 바 */}
        <View style={styles.progressBarContainer}>
          <View style={styles.progressBarBackground}>
            <View 
              style={[
                styles.progressBarFill, 
                { 
                  width: `${winRateNum}%`, 
                  backgroundColor: winRateColor 
                }
              ]} 
            />
          </View>
        </View>

        {/* 상세 기록 */}
        <View style={styles.recordContainer}>
          <View style={styles.recordItem}>
            <Text style={[styles.recordLabel, !hasData && { color: '#CCCCCC' }]}>총 경기</Text>
            <Text style={[styles.recordValue, !hasData && { color: '#9E9E9E' }]}>{stadium.matches}경기</Text>
          </View>
          <View style={styles.recordItem}>
            <Text style={[styles.recordLabel, !hasData && { color: '#CCCCCC' }]}>승</Text>
            <Text style={[styles.recordValue, { color: hasData ? '#4CAF50' : '#9E9E9E' }]}>{stadium.wins}승</Text>
          </View>
          <View style={styles.recordItem}>
            <Text style={[styles.recordLabel, !hasData && { color: '#CCCCCC' }]}>무</Text>
            <Text style={[styles.recordValue, { color: hasData ? '#9E9E9E' : '#9E9E9E' }]}>{stadium.draws || 0}무</Text>
          </View>
          <View style={styles.recordItem}>
            <Text style={[styles.recordLabel, !hasData && { color: '#CCCCCC' }]}>패</Text>
            <Text style={[styles.recordValue, { color: hasData ? '#F44336' : '#9E9E9E' }]}>{stadium.losses}패</Text>
          </View>
        </View>
      </View>
    );
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

      {/* 구장별 카드 리스트 */}
      <ScrollView showsVerticalScrollIndicator={false}>
        {getStadiumData().map(renderStadiumCard)}
      </ScrollView>
    </View>
  );
};
