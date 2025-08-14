import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  RefreshControl,
  Alert,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { StackNavigationProp, RouteProp } from '@react-navigation/stack';
import type { CompositeNavigationProp } from '@react-navigation/native';
import { chatRoomApi } from '../../entities/chat-room/api/api';
import { gameApi } from '../../entities/game/api/api';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { Game } from '../../entities/game/api/types';
import type { ChatStackParamList, RootStackParamList } from '../../navigation/types';
import { useUserStore } from '../../entities/user/model/userStore';
import { useTokenStore } from '../../shared/api/token/tokenStore';
import { useAttendanceStore } from '../../entities/attendance/model/attendanceStore';
import { useThemeColor } from '../../shared/team/ThemeContext';
import { BaseballAnimation } from '../../features/match-chat/components/BaseballAnimation';
import { styles } from './MatchChatRoomListScreen.styles';

type NavigationProp = CompositeNavigationProp<
  StackNavigationProp<ChatStackParamList>,
  StackNavigationProp<RootStackParamList>
>;
type RoutePropType = RouteProp<ChatStackParamList, 'MatchChatRoomList'>;

export const MatchChatRoomListScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<RoutePropType>();
  const getCurrentUser = useUserStore((state) => state.getCurrentUser);
  const { getAccessToken } = useTokenStore();
  const { isVerifiedToday } = useAttendanceStore();
  const [rooms, setRooms] = useState<MatchChatRoom[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [showAnimation, setShowAnimation] = useState(false);
  const [gameInfoMap, setGameInfoMap] = useState<Map<string, Game>>(new Map());
  const themeColor = useThemeColor();
  const insets = useSafeAreaInsets();

  const getTeamInfo = (teamId: string | number) => {
    const teamInfoMap: { [key: string]: { name: string; color: string } } = {
      // 숫자 ID로 매핑 (teamTypes.ts 기준)
      '1': { name: '한화', color: '#FF6600' },
      '2': { name: 'LG', color: '#C30452' },
      '3': { name: '롯데', color: '#002955' },
      '4': { name: 'KT', color: '#000000' },
      '5': { name: '삼성', color: '#0066B3' },
      '6': { name: 'KIA', color: '#EA0029' },
      '7': { name: 'SSG', color: '#CE0E2D' },
      '8': { name: 'NC', color: '#1D467F' },
      '9': { name: '두산', color: '#131230' },
      '10': { name: '키움', color: '#820024' },
      // 문자열 ID도 지원 (기존 호환성)
      'LG': { name: 'LG', color: '#C30452' },
      '두산': { name: '두산', color: '#131230' },
      'KIA': { name: 'KIA', color: '#EA0029' },
      '삼성': { name: '삼성', color: '#074CA1' },
      '롯데': { name: '롯데', color: '#041E42' },
      'SSG': { name: 'SSG', color: '#CE0E2D' },
      'KT': { name: 'KT', color: '#000000' },
      '한화': { name: '한화', color: '#FF6600' },
      'NC': { name: 'NC', color: '#315288' },
      '키움': { name: '키움', color: '#570514' },
    };
    const key = String(teamId);
    return teamInfoMap[key] || { name: `팀 ${teamId}`, color: '#007AFF' };
  };

  const loadGameInfo = async (gameId: string) => {
    try {
      
      if (gameInfoMap.has(gameId)) {
        return gameInfoMap.get(gameId);
      }
      
      const response = await gameApi.getGameById(gameId);
      
      if (response.status === 'SUCCESS') {
        setGameInfoMap(prevMap => {
          const newMap = new Map(prevMap);
          newMap.set(gameId, response.data);
          return newMap;
        });
        
        return response.data;
      }
    } catch (error) {
      console.error(`게임 정보 로드 실패 (gameId: ${gameId}):`, error);
    }
    return null;
  };

  const loadRooms = async (isRefresh = false, cursor?: string | null) => {
    try {
      if (isRefresh) {
        setLoading(true);
        setRooms([]);
        setNextCursor(null);
        setHasMore(true);
      } else if (!isRefresh && cursor) {
        setLoadingMore(true);
      } else if (!isRefresh && !cursor && rooms.length === 0) {
        setLoading(true);
      }

      const response = await chatRoomApi.getMatchChatRooms({
        lastCreatedAt: cursor || undefined,
        limit: 20 // 한 번에 20개씩 로드
      });
      
      let roomList: MatchChatRoom[] = [];
      let responseHasMore = false;
      let responseNextCursor: string | null = null;

      // 백엔드 응답 형식 처리
      if (response.data?.data?.chatRooms || response.data?.data?.rooms) {
        const data = response.data.data;
        roomList = data.chatRooms || data.rooms || [];
        responseHasMore = data.hasMore || false;
        responseNextCursor = data.nextCursor || null;
      } else if (response.data?.rooms) {
        // 목 데이터 형식 (기존 호환성)
        const data = response.data;
        roomList = data.rooms || [];
        responseHasMore = data.hasMore || false;
        responseNextCursor = data.nextCursor || null;
      } else {
        console.warn('예상하지 못한 응답 형식:', response);
        roomList = [];
      }
      
      // 방 목록 업데이트
      if (isRefresh || (!cursor && rooms.length === 0)) {
        setRooms(roomList);
      } else {
        setRooms(prev => [...prev, ...roomList]);
      }
      
      setHasMore(responseHasMore);
      setNextCursor(responseNextCursor);
      
      // 각 방의 게임 정보 로드
      const gameIds = roomList
        .filter(room => room.gameId)
        .map(room => String(room.gameId!))
        .filter((gameId, index, self) => self.indexOf(gameId) === index);
      
      for (const gameId of gameIds) {
        await loadGameInfo(gameId);
      }
    } catch (error) {
      console.error('채팅방 목록 로드 실패:', error);
      if (isRefresh || rooms.length === 0) {
        Alert.alert('오류', '채팅방 목록을 불러오는데 실패했습니다.');
      }
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await loadRooms(true); // isRefresh = true
    setRefreshing(false);
  };

  const loadMore = async () => {
    if (!hasMore || loadingMore || loading) return;
    await loadRooms(false, nextCursor);
  };


  const handleWatchChatJoin = async () => {
    // 직관 인증 상태 확인
    if (!isVerifiedToday()) {
      Alert.alert(
        '직관 인증 필요',
        '직관 채팅은 직관 인증 후에만 사용할 수 있습니다.\n직관 인증을 먼저 진행해주세요.',
        [
          {
            text: '취소',
            style: 'cancel',
          },
          {
            text: '직관 인증하기',
            onPress: () => navigation.navigate('AttendanceVerification'),
          },
        ]
      );
      return;
    }

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

      // 오늘의 게임 정보 가져오기
      
      const todayGameResponse = await gameApi.getTodayGame();
      
      if (todayGameResponse.status !== 'SUCCESS') {
        Alert.alert('오류', '오늘의 경기 정보를 가져올 수 없습니다.');
        return;
      }

      const todayGame = todayGameResponse.data;
      

      const watchRequest = {
        gameId: todayGame.gameId,
        teamId: currentUser.teamId,
        isAttendanceVerified: isVerifiedToday()
      };

      
      const response = await chatRoomApi.joinWatchChat(watchRequest);
      
      
      if (response.data.status === 'SUCCESS') {
        // 게임 정보 로드
        const gameDetails = await loadGameInfo(todayGame.gameId.toString());
        if (!gameDetails) {
          Alert.alert('오류', '게임 정보를 불러올 수 없습니다.');
          return;
        }

        const watchChatRoom = {
          matchId: `watch_chat_${todayGame.gameId}_${currentUser.teamId}`,
          gameId: todayGame.gameId.toString(),
          matchTitle: `직관채팅 - ${gameDetails.awayTeamName} vs ${gameDetails.homeTeamName}`,
          matchDescription: `${gameDetails.stadium}에서 열리는 경기를 함께 시청하며 채팅하는 공간`,
          teamId: getTeamInfo(currentUser.teamId).name,
          minAge: 0,
          maxAge: 100,
          genderCondition: 'ALL',
          maxParticipants: 999,
          currentParticipants: 0,
          createdAt: new Date().toISOString(),
          status: 'ACTIVE',
          websocketUrl: response.data.data.websocketUrl,
        };

        navigation.navigate('MatchChatRoom', {
          room: watchChatRoom,
          websocketUrl: response.data.data.websocketUrl,
          sessionToken: response.data.data.sessionToken,
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
    // 토큰 로그 출력
    const token = getAccessToken();
    
    
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

  const formatGameDateTime = (dateTimeString: string) => {
    const date = new Date(dateTimeString);
    return date.toLocaleString('ko-KR', {
      month: 'numeric',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const renderRoomItem = ({ item }: { item: MatchChatRoom }) => {
    const teamInfo = getTeamInfo(item.teamId);
    const gameInfo = item.gameId ? gameInfoMap.get(String(item.gameId)) : null;
  
    
    return (
      <TouchableOpacity
        style={styles.roomItem}
        onPress={() => navigation.navigate('MatchChatRoomDetail', { room: item })}
        activeOpacity={0.9}
      >
        <View style={styles.topSection}>
          <LinearGradient
            colors={['#049fbb', '#50f6ff']}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.gradientBackground}
          />
          
          
          {/* 헤더 영역 - 팀 배지만 */}
          <View style={styles.simpleHeader}>
            <View style={[styles.teamBadge, { backgroundColor: teamInfo.color }]}>
              <Text style={styles.teamText}>{teamInfo.name}</Text>
            </View>
          </View>

          {/* 메인 컨텐츠 영역 */}
          <View style={styles.centeredContent}>
            <Text style={styles.roomTitle}>{item.matchTitle}</Text>
            
            {gameInfo && (
              <View style={styles.gameInfoMain}>
                <Text style={styles.gameTeamsText}>
                  {gameInfo.awayTeamName} vs {gameInfo.homeTeamName}
                </Text>
                <Text style={styles.gameDetailsText}>
                  {formatGameDateTime(gameInfo.dateTime)} | {gameInfo.stadium}
                </Text>
              </View>
            )}
            
            <Text style={styles.roomDescription} numberOfLines={2}>
              {item.matchDescription}
            </Text>
          </View>
        </View>

        {/* 하단 정보 영역 */}
        <View style={styles.compactBottomInfo}>
          <Text style={styles.ageGenderInfo}>
            {item.minAge}-{item.maxAge}세 • {item.genderCondition === 'ALL' ? '전체' : 
             item.genderCondition === 'MALE' ? '남성' : '여성'}
          </Text>
          <Text style={styles.timeInfo}>
            {formatDate(item.createdAt)}
          </Text>
        </View>
      </TouchableOpacity>
    );
  };

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
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={[styles.header, { backgroundColor: themeColor }]}>
        <View style={styles.headerContent}>
          <Text style={styles.headerTitle}>매칭채팅</Text>
        </View>
        <View style={styles.headerButtons}>
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
          <TouchableOpacity
            style={styles.searchButton}
            onPress={() => navigation.navigate('ChatRoomSearch')}
            activeOpacity={0.7}
          >
            <Text style={styles.searchButtonText}>⌕</Text>
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
        onEndReached={loadMore}
        onEndReachedThreshold={0.1}
        ListFooterComponent={
          loadingMore ? (
            <View style={styles.loadingFooter}>
              <Text style={styles.loadingText}>더 많은 채팅방 로드 중...</Text>
            </View>
          ) : null
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
    </View>
  );
};

