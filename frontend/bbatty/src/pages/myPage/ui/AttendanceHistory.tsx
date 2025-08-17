import React from 'react';
import { View, Text, FlatList, TouchableOpacity, ActivityIndicator } from 'react-native';
import { AttendanceRecord } from '../../../features/user-profile/model/statsTypes';
import { useAttendanceHistory } from '../../../features/user-profile/hooks/useAttendanceHistory';
import { Season } from '../../../shared/utils/date';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { useUserStore } from '../../../entities/user/model/userStore';
import { nameToServerTeamId } from '../../../shared/team/teamIdMap';
import { styles } from './AttendanceHistory.style';

interface AttendanceHistoryProps {
  userId?: number;
  season?: Season;
  onRecordPress?: (record: AttendanceRecord) => void;
}

export const AttendanceHistory: React.FC<AttendanceHistoryProps> = ({ userId, season = 'total', onRecordPress }) => {
  const themeColor = useThemeColor();
  const currentUser = useUserStore((s) => s.currentUser);
  const userTeamId = currentUser?.teamId;

  // season이 유효하지 않으면 'total'로 기본값 설정
  const validSeason = season || 'total';

  const {
    data: records,
    isLoading,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
    refresh,
    isError,
  } = useAttendanceHistory(userId, validSeason);
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return `${date.getMonth() + 1}.${String(date.getDate()).padStart(2, '0')}`;
  };

  const getShortTeamName = (fullTeamName: string) => {
    // 띄어쓰기 앞의 핵심 이름만 추출
    return fullTeamName.split(' ')[0];
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'FINISHED':
        return '종료';
      case 'ONGOING':
        return '진행중';
      case 'SCHEDULED':
        return '예정';
      default:
        return status;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'FINISHED':
        return '#4CAF50';
      case 'ONGOING':
        return '#FF9800';
      case 'SCHEDULED':
        return '#2196F3';
      default:
        return '#666666';
    }
  };

  const renderRecord = ({ item }: { item: AttendanceRecord }) => {
    // 팀 ID 매핑
    const homeTeamId = nameToServerTeamId(item.homeTeam);
    const awayTeamId = nameToServerTeamId(item.awayTeam);

    // 사용자 팀 확인
    const isUserHomeTeam = userTeamId === homeTeamId;
    const isUserAwayTeam = userTeamId === awayTeamId;

    // 게임 결과 확인 (스코어가 있을 때만)
    const hasScores =
      item.homeScore !== null &&
      item.homeScore !== undefined &&
      item.awayScore !== null &&
      item.awayScore !== undefined;

    let gameResult: 'win' | 'lose' | 'draw' | 'none' = 'none';
    if (hasScores && (isUserHomeTeam || isUserAwayTeam)) {
      if (item.homeScore === item.awayScore) {
        gameResult = 'draw';
      } else if (isUserHomeTeam) {
        gameResult = item.homeScore! > item.awayScore! ? 'win' : 'lose';
      } else {
        gameResult = item.awayScore! > item.homeScore! ? 'win' : 'lose';
      }
    }

    // 스코어 색상 결정
    const getScoreColor = (isUserTeamScore: boolean, isOtherTeamScore: boolean) => {
      if (!hasScores || (!isUserHomeTeam && !isUserAwayTeam)) {
        // 스코어가 없거나 사용자 팀이 참여하지 않은 경우
        return isUserTeamScore ? themeColor : '#CCCCCC';
      }

      switch (gameResult) {
        case 'win':
          // 사용자 팀이 이긴 경우
          return isUserTeamScore ? themeColor : '#CCCCCC';
        case 'lose':
          // 사용자 팀이 진 경우
          return isUserTeamScore ? '#999999' : '#666666';
        case 'draw':
          // 비긴 경우 - 둘 다 회색
          return '#999999';
        default:
          return isUserTeamScore ? themeColor : '#CCCCCC';
      }
    };

    // 홈팀이 오른쪽에 오도록 배치
    const leftTeam = item.awayTeam;
    const rightTeam = item.homeTeam;
    const leftScore = item.awayScore;
    const rightScore = item.homeScore;

    const leftScoreColor = getScoreColor(isUserAwayTeam, isUserHomeTeam);
    const rightScoreColor = getScoreColor(isUserHomeTeam, isUserAwayTeam);

    // 팀명 단축
    const shortLeftTeam = getShortTeamName(leftTeam);
    const shortRightTeam = getShortTeamName(rightTeam);

    return (
      <TouchableOpacity style={styles.recordItem} onPress={() => onRecordPress?.(item)} activeOpacity={0.7}>
        {/* 왼쪽 스코어 (awayTeam) */}
        <View style={styles.scoreSection}>
          <Text style={[styles.scoreNumber, { color: leftScoreColor }]}>
            {leftScore !== null && leftScore !== undefined ? leftScore : '-'}
          </Text>
        </View>

        {/* 중앙 매치 정보 */}
        <View style={styles.matchInfo}>
          <Text style={styles.teams}>
            {shortLeftTeam} vs {shortRightTeam}
          </Text>
          <Text style={styles.date}>{formatDate(item.dateTime)}</Text>
          <Text style={styles.stadium}>{item.stadium}</Text>
        </View>

        {/* 오른쪽 스코어 (homeTeam) */}
        <View style={styles.scoreSection}>
          <Text style={[styles.scoreNumber, { color: rightScoreColor }]}>
            {rightScore !== null && rightScore !== undefined ? rightScore : '-'}
          </Text>
        </View>
      </TouchableOpacity>
    );
  };

  const renderFooter = () => {
    if (!isFetchingNextPage) return null;

    return (
      <View style={styles.footerLoader}>
        <ActivityIndicator size='small' color={themeColor} />
        <Text style={styles.footerText}>더 불러오는 중...</Text>
      </View>
    );
  };

  const handleEndReached = () => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  };

  if (isLoading) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <ActivityIndicator size='large' color={themeColor} />
        <Text style={styles.loadingText}>직관 기록을 불러오는 중...</Text>
      </View>
    );
  }

  if (isError) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <Text style={styles.errorText}>직관 기록을 불러오는데 실패했습니다.</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {records.length === 0 ? (
        <View style={styles.emptyState}>
          <Text style={styles.emptyText}>아직 직관 기록이 없습니다</Text>
        </View>
      ) : (
        <FlatList
          data={records}
          renderItem={renderRecord}
          keyExtractor={(item) => item.gameId.toString()}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.listContainer}
          onEndReached={handleEndReached}
          onEndReachedThreshold={0.1}
          ListFooterComponent={renderFooter}
          refreshing={false}
          onRefresh={refresh}
        />
      )}
    </View>
  );
};
