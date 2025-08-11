import React from 'react';
import { View, Text, FlatList, TouchableOpacity, ActivityIndicator } from 'react-native';
import { AttendanceRecord } from '../../../features/user-profile/model/statsTypes';
import { useAttendanceHistory } from '../../../features/user-profile/hooks/useAttendanceHistory';
import { Season } from '../../../shared/utils/date';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { styles } from './AttendanceHistory.style';

interface AttendanceHistoryProps {
  userId?: number;
  season?: Season;
  onRecordPress?: (record: AttendanceRecord) => void;
}

export const AttendanceHistory: React.FC<AttendanceHistoryProps> = ({ userId, season = 'total', onRecordPress }) => {
  const themeColor = useThemeColor();
  const {
    data: records,
    isLoading,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
    refresh,
    isError,
  } = useAttendanceHistory(userId, season);
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return `${date.getMonth() + 1}.${String(date.getDate()).padStart(2, '0')}`;
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

  const renderRecord = ({ item }: { item: AttendanceRecord }) => (
    <TouchableOpacity style={styles.recordItem} onPress={() => onRecordPress?.(item)} activeOpacity={0.7}>
      {/* 중앙 매치 정보 */}
      <View style={styles.matchInfo}>
        <Text style={styles.teams}>
          {item.homeTeam} vs {item.awayTeam}
        </Text>
        <Text style={styles.date}>{formatDate(item.dateTime)}</Text>
        <Text style={styles.stadium}>{item.stadium}</Text>
      </View>

      {/* 게임 상태 배지 */}
      <View style={[styles.statusBadge, { backgroundColor: getStatusColor(item.status) }]}>
        <Text style={styles.statusText}>{getStatusText(item.status)}</Text>
      </View>
    </TouchableOpacity>
  );

  const renderFooter = () => {
    if (!isFetchingNextPage) return null;
    
    return (
      <View style={styles.footerLoader}>
        <ActivityIndicator size="small" color={themeColor} />
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
        <ActivityIndicator size="large" color={themeColor} />
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
