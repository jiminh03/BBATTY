import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  SafeAreaView,
  RefreshControl,
  Alert,
  TextInput,
  Keyboard,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { chatRoomApi } from '../../entities/chat-room/api/api';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { ChatStackParamList } from '../../navigation/types';
import { useUserStore } from '../../entities/user/model/userStore';
import { useThemeColor } from '../../shared/team/ThemeContext';
import { BaseballAnimation } from '../../features/match-chat/components/BaseballAnimation';
import { styles } from './MatchChatRoomListScreen.styles';

type NavigationProp = StackNavigationProp<ChatStackParamList>;

export const MatchChatRoomListScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const getCurrentUser = useUserStore((state) => state.getCurrentUser);
  const [rooms, setRooms] = useState<MatchChatRoom[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [showAnimation, setShowAnimation] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [isSearchMode, setIsSearchMode] = useState(false);
  const themeColor = useThemeColor();

  const getTeamColors = (teamId: string) => {
    const teamColorMap: { [key: string]: string } = {
      'LG': '#C30452',
      '두산': '#131230',
      'KIA': '#EA0029',
      '삼성': '#074CA1',
      '롯데': '#041E42',
      'SSG': '#CE0E2D',
      'KT': '#000000',
      '한화': '#FF6600',
      'NC': '#315288',
      '키움': '#570514',
    };
    return teamColorMap[teamId] || '#007AFF';
  };

  const loadRooms = async (keyword?: string) => {
    try {
      setLoading(true);
      const response = await chatRoomApi.getMatchChatRooms(keyword);
      
      if (response.data?.data?.chatRooms) {
        setRooms(response.data.data.chatRooms);
      } else if (response.data?.data?.rooms) {
        setRooms(response.data.data.rooms);
      } else if (response.data?.rooms) {
        // 목 데이터 형식 (기존 호환성)
        setRooms(response.data.rooms);
      } else {
        Alert.alert('오류', '채팅방 목록을 불러오는데 실패했습니다.');
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
    await loadRooms(isSearchMode ? searchKeyword : undefined);
    setRefreshing(false);
  };

  const handleSearch = async (text: string) => {
    setSearchKeyword(text);
    if (text.trim()) {
      setIsSearchMode(true);
      await loadRooms(text.trim());
    } else {
      setIsSearchMode(false);
      await loadRooms();
    }
  };

  const toggleSearchMode = () => {
    if (isSearchMode) {
      setIsSearchMode(false);
      setSearchKeyword('');
      loadRooms();
      Keyboard.dismiss();
    } else {
      setIsSearchMode(true);
    }
  };

  const handleWatchChatJoin = async () => {
    // 애니메이션 시작
    setShowAnimation(true);
  };

  const onNavigateToChat = async () => {
    try {
      const currentUser = getCurrentUser();
      
      if (!currentUser) {
        Alert.alert('오류', '사용자 정보를 찾을 수 없습니다.');
        return;
      }

      const watchRequest = {
        gameId: 1258,
        teamId: currentUser.teamId,
        isAttendanceVerified: true
      };

      const response = await chatRoomApi.joinWatchChat(watchRequest);
      console.log('Watch chat API response:', response.data);
      
      if (response.data.status === 'SUCCESS') {
        // 워치 채팅방으로 이동 (매치 채팅과 동일한 화면 사용)
        navigation.navigate('MatchChatRoom', {
          room: {
            matchId: 'watch_chat_' + Date.now(),
            gameId: watchRequest.gameId.toString(),
            matchTitle: '직관채팅',
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

  const onAnimationComplete = () => {
    setShowAnimation(false);
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
        <View style={styles.titleContainer}>
          <Text style={styles.roomTitle}>{item.matchTitle}</Text>
          <Text style={styles.roomDescription} numberOfLines={1}>
            {item.matchDescription}
          </Text>
        </View>
        <View style={[styles.teamBadge, { backgroundColor: getTeamColors(item.teamId) }]}>
          <Text style={styles.teamText}>{item.teamId}</Text>
        </View>
      </View>
      
      <View style={styles.roomInfo}>
        <View style={styles.infoItem}>
          <Text style={styles.infoLabel}>연령</Text>
          <Text style={styles.infoValue}>{item.minAge}-{item.maxAge}세</Text>
        </View>
        <View style={styles.infoItem}>
          <Text style={styles.infoLabel}>참여자</Text>
          <Text style={[styles.infoValue, styles.participantCount]}>
            {item.currentParticipants}/{item.maxParticipants}
          </Text>
        </View>
        <View style={styles.infoItem}>
          <Text style={styles.infoLabel}>성별</Text>
          <Text style={styles.infoValue}>
            {item.genderCondition === 'ALL' ? '전체' : 
             item.genderCondition === 'MALE' ? '남성' : '여성'}
          </Text>
        </View>
      </View>
      
      <View style={styles.roomFooter}>
        <Text style={styles.createdAt}>
          {formatDate(item.createdAt)}
        </Text>
        <View style={[
          styles.statusBadge, 
          item.status === 'ACTIVE' ? styles.activeBadge : styles.inactiveBadge
        ]}>
          <Text style={styles.statusText}>
            {item.status === 'ACTIVE' ? '모집중' : '마감'}
          </Text>
        </View>
      </View>
    </TouchableOpacity>
  );

  const EmptyComponent = () => (
    <View style={styles.emptyContainer}>
      <View style={styles.emptyIconContainer}>
        <Text style={styles.emptyIcon}>📝</Text>
      </View>
      <Text style={styles.emptyText}>아직 생성된 매치룸이 없습니다</Text>
      <Text style={styles.emptySubtext}>첫 번째 매치룸을 만들어보세요!</Text>
      <TouchableOpacity
        style={[styles.createButton, { backgroundColor: themeColor }]}
        onPress={() => navigation.navigate('CreateMatchChatRoom')}
      >
        <Text style={styles.createButtonText}>매치룸 개설하기</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={[styles.header, { backgroundColor: themeColor }]}>
        <View style={styles.headerContent}>
          <Text style={styles.headerTitle}>매칭채팅</Text>
        </View>
        <View style={styles.headerButtons}>
          <TouchableOpacity
            style={styles.searchButton}
            onPress={toggleSearchMode}
          >
            <Text style={styles.searchButtonText}>
              {isSearchMode ? '취소' : '검색'}
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.watchChatButton}
            onPress={() => handleWatchChatJoin()}
          >
            <Text style={styles.watchChatButtonText}>직관채팅</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.headerButton}
            onPress={() => navigation.navigate('CreateMatchChatRoom')}
          >
            <Text style={styles.headerButtonText}>매치룸 개설</Text>
          </TouchableOpacity>
        </View>
      </View>

      {isSearchMode && (
        <View style={styles.searchContainer}>
          <TextInput
            style={styles.searchInput}
            placeholder="채팅방 제목, 설명, 팀명으로 검색..."
            placeholderTextColor="#999"
            value={searchKeyword}
            onChangeText={handleSearch}
            autoFocus={true}
            returnKeyType="search"
            onSubmitEditing={() => Keyboard.dismiss()}
          />
        </View>
      )}

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
      
      {/* 야구 애니메이션 */}
      {showAnimation && (
        <BaseballAnimation 
          onAnimationComplete={onAnimationComplete}
          onNavigate={onNavigateToChat}
        />
      )}
    </SafeAreaView>
  );
};

