import React from 'react';
import { View, Text, FlatList, TouchableOpacity } from 'react-native';
import { DirectViewRecord } from '../../../features/user-stats/model/statsTypes';
import { styles } from './AttendanceHistory.style';

interface AttendanceHistoryProps {
  records: DirectViewRecord[];
  onRecordPress?: (record: DirectViewRecord) => void;
}

export const AttendanceHistory: React.FC<AttendanceHistoryProps> = ({ records, onRecordPress }) => {
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return `${date.getMonth() + 1}/${date.getDate()}`;
  };

  const getResultText = (record: DirectViewRecord) => {
    if (record.homeScore === record.awayScore) return '무';
    return record.isWin ? '승' : '패';
  };

  const getResultColor = (record: DirectViewRecord) => {
    if (record.homeScore === record.awayScore) return '#FFC107';
    return record.isWin ? '#4CAF50' : '#F44336';
  };

  const renderRecord = ({ item }: { item: DirectViewRecord }) => (
    <TouchableOpacity style={styles.recordItem} onPress={() => onRecordPress?.(item)} activeOpacity={0.7}>
      <View style={styles.scoreSection}>
        <Text style={styles.score}>{item.homeScore}</Text>
      </View>

      <View style={styles.matchInfo}>
        <Text style={styles.teams}>
          {item.homeTeam} vs {item.awayTeam}
        </Text>
        <Text style={styles.date}>{formatDate(item.date)}</Text>
      </View>

      <View style={styles.scoreSection}>
        <Text style={styles.score}>{item.awayScore}</Text>
      </View>
    </TouchableOpacity>
  );

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
          keyExtractor={(item) => item.id.toString()}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.listContainer}
        />
      )}
    </View>
  );
};
