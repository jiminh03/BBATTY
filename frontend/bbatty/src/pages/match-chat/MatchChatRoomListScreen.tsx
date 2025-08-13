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
import { chatRoomApi } from '../../entities/chat-room/api/api';
import { gameApi } from '../../entities/game/api/api';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { Game } from '../../entities/game/api/types';
import type { ChatStackParamList } from '../../navigation/types';
import { useUserStore } from '../../entities/user/model/userStore';
import { useTokenStore } from '../../shared/api/token/tokenStore';
import { useThemeColor } from '../../shared/team/ThemeContext';
import { BaseballAnimation } from '../../features/match-chat/components/BaseballAnimation';
import { styles } from './MatchChatRoomListScreen.styles';

type NavigationProp = StackNavigationProp<ChatStackParamList>;
type RoutePropType = RouteProp<ChatStackParamList, 'MatchChatRoomList'>;

export const MatchChatRoomListScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<RoutePropType>();
  const getCurrentUser = useUserStore((state) => state.getCurrentUser);
  const { getAccessToken } = useTokenStore();
  const [rooms, setRooms] = useState<MatchChatRoom[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
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
        console.log('🎮 게임 정보 저장됨:', gameId, response.data);
        return response.data;
      }
    } catch (error) {
      console.error(`게임 정보 로드 실패 (gameId: ${gameId}):`, error);
    }
    return null;
  };

  const loadRooms = async () => {
    try {
      setLoading(true);
      const response = await chatRoomApi.getMatchChatRooms();
      
      let roomList: MatchChatRoom[] = [];
      if (response.data?.data?.chatRooms) {
        roomList = response.data.data.chatRooms;
      } else if (response.data?.data?.rooms) {
        roomList = response.data.data.rooms;
      } else if (response.data?.rooms) {
        // 목 데이터 형식 (기존 호환성)
        roomList = response.data.rooms;
      } else {
        Alert.alert('오류', '채팅방 목록을 불러오는데 실패했습니다.');
        return;
      }
      
      setRooms(roomList);
      
      // 각 방의 게임 정보 로드
      const gameIds = roomList
        .filter(room => room.gameId)
        .map(room => String(room.gameId!)) // number를 string으로 변환
        .filter((gameId, index, self) => self.indexOf(gameId) === index); // 중복 제거
      
      console.log('🎮 로드할 게임 ID들:', gameIds);
      
      for (const gameId of gameIds) {
        await loadGameInfo(gameId);
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
      console.log('🎮 오늘의 게임 정보 조회 중...');
      const todayGameResponse = await gameApi.getTodayGame();
      
      if (todayGameResponse.status !== 'SUCCESS') {
        Alert.alert('오류', '오늘의 경기 정보를 가져올 수 없습니다.');
        return;
      }

      const todayGame = todayGameResponse.data;
      console.log('🎮 오늘의 게임 정보:', todayGame);

      const watchRequest = {
        gameId: todayGame.gameId,
        teamId: currentUser.teamId,
        isAttendanceVerified: true
      };

      console.log('🎮 직관채팅 참여 요청:', watchRequest);
      const response = await chatRoomApi.joinWatchChat(watchRequest);
      console.log('Watch chat API response:', response.data);
      
      if (response.data.status === 'SUCCESS') {
        // 워치 채팅방으로 이동
        navigation.push('MatchChatRoom', {
          room: {
            matchId: 'watch_chat_' + Date.now(),
            gameId: watchRequest.gameId.toString(),
            matchTitle: `직관채팅 - ${todayGame.awayTeamName} vs ${todayGame.homeTeamName}`,
            matchDescription: `${todayGame.stadium}에서 열리는 경기를 함께 시청하며 채팅하는 공간`,
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

  const handleDirectWatchConnection = async (connectionInfo: {
    gameId: number;
    teamId: number;
    isAttendanceVerified: boolean;
  }) => {
    try {
      const currentUser = getCurrentUser();
      
      if (!currentUser) {
        Alert.alert('오류', '사용자 정보를 찾을 수 없습니다.');
        return;
      }

      console.log('🎯 직관채팅 직접 연결 시작:', connectionInfo);

      const watchRequest = {
        gameId: connectionInfo.gameId,
        teamId: connectionInfo.teamId,
        isAttendanceVerified: connectionInfo.isAttendanceVerified,
      };

      console.log('🎮 직관채팅 연결 요청:', watchRequest);

      const response = await chatRoomApi.connectWatchChat(watchRequest);
      console.log('🎮 직관채팅 연결 응답:', response);

      if (response.status === 'SUCCESS') {
        console.log('✅ 직관채팅 연결 성공 - 채팅방으로 이동');
        
        navigation.navigate('MatchChatRoom', {
          roomId: response.data.roomId,
          roomType: 'WATCH',
          gameId: connectionInfo.gameId,
        });
      } else {
        console.error('❌ 직관채팅 연결 실패:', response.message);
        Alert.alert('연결 실패', response.message || '직관채팅 연결에 실패했습니다.');
      }
    } catch (error) {
      console.error('❌ 직관채팅 연결 중 오류:', error);
      Alert.alert('오류', '직관채팅 연결 중 문제가 발생했습니다.');
    }
  };

  useEffect(() => {
    // 토큰 로그 출력
    const token = getAccessToken();
    console.log('🔑 매칭채팅 목록 진입 - 토큰:', token);
    
    loadRooms();
  }, []);

  // 직접 직관채팅 연결 처리
  useEffect(() => {
    const params = route.params as any;
    if (params?.directWatchConnection) {
      console.log('🎯 직접 직관채팅 연결 요청:', params.directWatchConnection);
      handleDirectWatchConnection(params.directWatchConnection);
    }
  }, [route.params]);

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

