import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  SafeAreaView,
  RefreshControl,
  Alert,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { chatRoomApi } from '../../entities/chat-room/api/api';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { ChatStackParamList } from '../../navigation/types';

type NavigationProp = StackNavigationProp<ChatStackParamList>;

export const MatchChatRoomListScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const [rooms, setRooms] = useState<MatchChatRoom[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const loadRooms = async () => {
    try {
      setLoading(true);
      const response = await chatRoomApi.getMatchChatRooms();
      
      // 실제 서버 응답 구조에 맞게 처리
      if (response.data.status === 'SUCCESS') {
        setRooms(response.data.data.rooms);
      } else {
        Alert.alert('오류', response.data.message || '채팅방 목록을 불러오는데 실패했습니다.');
      }
    } catch (error) {
      console.error('채팅방 목록 로드 실패:', error);
      Alert.alert('오류', '채팅방 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await loadRooms();
    setRefreshing(false);
  };

  const handleWatchChatJoin = async () => {
    try {
      const watchRequest = {
        gameId: "game_001", // 기본 워치 채팅 (실제로는 현재 경기 또는 선택 가능)
        isAttendanceVerified: true
      };

      const response = await chatRoomApi.joinWatchChat(watchRequest);
      
      if (response.data.status === 'SUCCESS') {
        // 워치 채팅방으로 이동 (매치 채팅과 동일한 화면 사용)
        navigation.navigate('MatchChatRoom', {
          room: {
            matchId: 'watch_chat_' + Date.now(),
            gameId: watchRequest.gameId,
            matchTitle: '📺 워치 채팅',
            matchDescription: '모든 팬들이 함께 경기를 시청하며 채팅하는 공간',
            teamId: '전체',
            minAge: 0,
            maxAge: 100,
            genderCondition: 'ALL',
            maxParticipants: 999,
            currentParticipants: 0,
            createdAt: new Date().toISOString(),
            status: 'ACTIVE',
            websocketUrl: response.data.data.websocketUrl
          },
          websocketUrl: response.data.data.websocketUrl,
          sessionToken: response.data.data.sessionToken
        });
      } else {
        Alert.alert('오류', response.data.message || '워치 채팅 참여에 실패했습니다.');
      }
    } catch (error) {
      console.error('워치 채팅 참여 실패:', error);
      Alert.alert('오류', '워치 채팅 참여에 실패했습니다.');
    }
  };

  useEffect(() => {
    loadRooms();
  }, []);

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (minutes < 60) return `${minutes}분 전`;
    if (hours < 24) return `${hours}시간 전`;
    return `${days}일 전`;
  };

  const renderRoomItem = ({ item }: { item: MatchChatRoom }) => (
    <TouchableOpacity
      style={styles.roomItem}
      onPress={() => navigation.navigate('MatchChatRoomDetail', { room: item })}
    >
      <View style={styles.roomHeader}>
        <Text style={styles.roomTitle}>{item.matchTitle}</Text>
        <View style={styles.teamBadge}>
          <Text style={styles.teamText}>{item.teamId}</Text>
        </View>
      </View>
      
      <Text style={styles.roomDescription} numberOfLines={2}>
        {item.matchDescription}
      </Text>
      
      <View style={styles.roomInfo}>
        <Text style={styles.ageRange}>
          {item.minAge}-{item.maxAge}세
        </Text>
        <Text style={styles.participants}>
          {item.currentParticipants}/{item.maxParticipants}명
        </Text>
        <Text style={styles.createdAt}>
          {formatDate(item.createdAt)}
        </Text>
      </View>
      
      <View style={styles.genderBadge}>
        <Text style={styles.genderText}>
          {item.genderCondition === 'ALL' ? '전체' : 
           item.genderCondition === 'MALE' ? '남성' : '여성'}
        </Text>
      </View>
    </TouchableOpacity>
  );

  const EmptyComponent = () => (
    <View style={styles.emptyContainer}>
      <Text style={styles.emptyText}>아직 생성된 채팅방이 없습니다</Text>
      <TouchableOpacity
        style={styles.createButton}
        onPress={() => navigation.navigate('CreateMatchChatRoom')}
      >
        <Text style={styles.createButtonText}>첫 채팅방 만들기</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>매치 채팅방</Text>
        <View style={styles.headerButtons}>
          <TouchableOpacity
            style={styles.watchChatButton}
            onPress={() => handleWatchChatJoin()}
          >
            <Text style={styles.watchChatButtonText}>📺 워치채팅</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.headerButton}
            onPress={() => navigation.navigate('CreateMatchChatRoom')}
          >
            <Text style={styles.headerButtonText}>+ 방 만들기</Text>
          </TouchableOpacity>
        </View>
      </View>

      <FlatList
        data={rooms}
        renderItem={renderRoomItem}
        keyExtractor={(item) => item.matchId}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
        ListEmptyComponent={!loading ? EmptyComponent : null}
        showsVerticalScrollIndicator={false}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  headerButtons: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  watchChatButton: {
    backgroundColor: '#4CAF50',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 16,
  },
  watchChatButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  headerButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  headerButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  listContent: {
    padding: 16,
  },
  roomItem: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  roomHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  roomTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    flex: 1,
    marginRight: 8,
  },
  teamBadge: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
  },
  teamText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  roomDescription: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
    marginBottom: 12,
  },
  roomInfo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  ageRange: {
    fontSize: 12,
    color: '#999',
  },
  participants: {
    fontSize: 12,
    color: '#007AFF',
    fontWeight: '600',
  },
  createdAt: {
    fontSize: 12,
    color: '#999',
  },
  genderBadge: {
    alignSelf: 'flex-start',
    backgroundColor: '#f0f0f0',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  genderText: {
    fontSize: 12,
    color: '#666',
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 60,
  },
  emptyText: {
    fontSize: 16,
    color: '#999',
    marginBottom: 20,
    textAlign: 'center',
  },
  createButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  createButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});