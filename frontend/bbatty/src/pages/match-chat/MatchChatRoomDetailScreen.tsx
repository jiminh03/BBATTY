import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  Alert,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import type { RouteProp } from '@react-navigation/native';
import { chatRoomApi } from '../../entities/chat-room/api/api';
import { gameApi } from '../../entities/game/api/api';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { Game } from '../../entities/game/api/types';
import type { ChatStackParamList } from '../../navigation/types';
import { useUserStore } from '../../entities/user/model/userStore';
import { useThemeColor } from '../../shared/team/ThemeContext';
import { styles } from './MatchChatRoomDetailScreen.styles';

type NavigationProp = StackNavigationProp<ChatStackParamList>;
type RoutePropType = RouteProp<ChatStackParamList, 'MatchChatRoomDetail'>;

export const MatchChatRoomDetailScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<RoutePropType>();
  const { room } = route.params;
  const getCurrentUser = useUserStore((state) => state.getCurrentUser);
  const themeColor = useThemeColor();
  const insets = useSafeAreaInsets();
  
  const [joining, setJoining] = useState(false);
  const [gameInfo, setGameInfo] = useState<Game | null>(null);

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
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

  const loadGameInfo = async () => {
    if (room.gameId && !gameInfo) {
      try {
        const response = await gameApi.getGameById(room.gameId);
        if (response.status === 'SUCCESS') {
          setGameInfo(response.data);
        }
      } catch (error) {
        console.error('게임 정보 로드 실패:', error);
      }
    }
  };

  const handleJoinRoom = async () => {
    try {
      setJoining(true);
      
      const currentUser = getCurrentUser();
      
      if (!currentUser) {
        Alert.alert('오류', '사용자 정보를 찾을 수 없습니다.');
        return;
      }
      
      const joinRequest = {
        matchId: room.matchId,
        nickname: currentUser.nickname,
        winRate: currentUser.winRate || 0,
        profileImgUrl: currentUser.profileImg || '',
        isWinFairy: (currentUser.winRate || 0) >= 70,
      };

      const response = await chatRoomApi.joinMatchChat(joinRequest);
      
      // 채팅방으로 이동
      if (response.status === 'SUCCESS') {
        navigation.navigate('MatchChatRoom', { 
          room,
          websocketUrl: response.data.websocketUrl,
          sessionToken: response.data.sessionToken
        });
      } else {
        Alert.alert('오류', response.message || '채팅방 참여에 실패했습니다.');
      }
      
    } catch (error) {
      console.error('채팅방 참여 실패:', error);
      Alert.alert('오류', '채팅방 참여에 실패했습니다.');
    } finally {
      setJoining(false);
    }
  };

  const getGenderText = (condition: string) => {
    switch (condition) {
      case 'ALL': return '전체';
      case 'MALE': return '남성만';
      case 'FEMALE': return '여성만';
      default: return '전체';
    }
  };

  const isRoomFull = room.currentParticipants >= room.maxParticipants;
  const isRoomActive = room.status === 'ACTIVE';

  const getTeamInfo = (teamId: string | number) => {
    const teamInfoMap: { [key: string]: { name: string; colors: string[] } } = {
      // 숫자 ID로 매핑 (teamTypes.ts 기준)
      '1': { name: '한화', colors: ['#FF6600', '#FF4500'] },
      '2': { name: 'LG', colors: ['#C30452', '#8B0000'] },
      '3': { name: '롯데', colors: ['#002955', '#000080'] },
      '4': { name: 'KT', colors: ['#000000', '#2F2F2F'] },
      '5': { name: '삼성', colors: ['#0066B3', '#0066CC'] },
      '6': { name: 'KIA', colors: ['#EA0029', '#DC143C'] },
      '7': { name: 'SSG', colors: ['#CE0E2D', '#B22222'] },
      '8': { name: 'NC', colors: ['#1D467F', '#4169E1'] },
      '9': { name: '두산', colors: ['#131230', '#000080'] },
      '10': { name: '키움', colors: ['#820024', '#8B0000'] },
      // 문자열 ID도 지원 (기존 호환성)
      'LG': { name: 'LG', colors: ['#C30452', '#8B0000'] },
      '두산': { name: '두산', colors: ['#131230', '#000080'] },
      'KIA': { name: 'KIA', colors: ['#EA0029', '#DC143C'] },
      '삼성': { name: '삼성', colors: ['#074CA1', '#0066CC'] },
      '롯데': { name: '롯데', colors: ['#041E42', '#000080'] },
      'SSG': { name: 'SSG', colors: ['#CE0E2D', '#B22222'] },
      'KT': { name: 'KT', colors: ['#000000', '#2F2F2F'] },
      '한화': { name: '한화', colors: ['#FF6600', '#FF4500'] },
      'NC': { name: 'NC', colors: ['#315288', '#4169E1'] },
      '키움': { name: '키움', colors: ['#570514', '#8B0000'] },
    };
    const key = String(teamId);
    return teamInfoMap[key] || { name: `팀 ${teamId}`, colors: ['#007AFF', '#0066CC'] };
  };

  const teamInfo = getTeamInfo(room.teamId);

  useEffect(() => {
    loadGameInfo();
  }, [room.gameId]);

  return (
    <View style={styles.container}>
      <LinearGradient
        colors={[themeColor, themeColor]}
        style={[styles.headerGradient, { paddingTop: insets.top }]}
      >
        <View style={styles.header}>
          <TouchableOpacity onPress={() => {
            if (navigation.canGoBack()) {
              navigation.goBack();
            }
          }}>
            <Text style={[styles.backButton, { color: '#ffffff' }]}>← 뒤로</Text>
          </TouchableOpacity>
          <View style={styles.headerCenter}>
            <Text style={[styles.headerTitle, { color: '#ffffff' }]}>매치룸 정보</Text>
            {gameInfo && (
              <Text style={[styles.headerSubtitle, { color: '#ffffff' }]}>
                {gameInfo.awayTeamName} vs {gameInfo.homeTeamName}
              </Text>
            )}
          </View>
          <View style={styles.placeholder} />
        </View>
      </LinearGradient>

      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.roomCard}>
          <View style={styles.cardGradient}>
            <View style={styles.gradientBackground} />
            
            
            {/* 헤더 영역 - 팀 배지만 */}
            <View style={styles.simpleRoomHeader}>
              <View style={[styles.teamBadge, { backgroundColor: teamInfo.colors[0] }]}>
                <Text style={styles.teamText}>{teamInfo.name}</Text>
              </View>
            </View>

            {/* 제목 영역 */}
            <View style={styles.detailTitleContainer}>
              <Text style={styles.roomTitle}>{room.matchTitle}</Text>
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
              <Text style={styles.roomDescription}>{room.matchDescription}</Text>
            </View>
          </View>
          
          <View style={styles.bottomContent}>
            {/* 추가적인 정보가 필요하다면 여기에 */}
          </View>
        </View>

        {/* 참여 조건 */}
        <View style={styles.conditionsSection}>
          <View style={styles.sectionHeader}>
            <View style={styles.headerIcon}>
              <Text style={styles.headerIconText}>👥</Text>
            </View>
            <Text style={styles.sectionTitle}>참여 조건</Text>
          </View>
          
          <View style={styles.conditionItem}>
            <Text style={styles.conditionLabel}>연령대</Text>
            <Text style={styles.conditionValue}>{room.minAge}-{room.maxAge}세</Text>
          </View>
          
          <View style={styles.conditionItem}>
            <Text style={styles.conditionLabel}>성별</Text>
            <Text style={styles.conditionValue}>{getGenderText(room.genderCondition)}</Text>
          </View>
        </View>

        {/* 추가 정보 */}
        {gameInfo && (
          <View style={styles.gameSection}>
            <View style={styles.sectionHeader}>
              <View style={styles.headerIcon}>
                <Text style={styles.headerIconText}>⚾</Text>
              </View>
              <Text style={styles.sectionTitle}>경기 정보</Text>
            </View>
            
            <View style={styles.gameContent}>
              <Text style={styles.gameTeams}>
                {gameInfo.awayTeamName} vs {gameInfo.homeTeamName}
              </Text>
              <Text style={styles.gameDetails}>
                {new Date(gameInfo.dateTime).toLocaleDateString('ko-KR', {
                  month: 'long',
                  day: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit'
                })} • {gameInfo.stadium}
              </Text>
            </View>
          </View>
        )}

        <View style={styles.actionSection}>
          <TouchableOpacity
            style={[styles.joinButton, { backgroundColor: themeColor }]}
            onPress={handleJoinRoom}
            disabled={joining}
            activeOpacity={0.8}
          >
            <Text style={styles.joinButtonText}>
              {joining ? '참여중...' : '채팅방 참여하기'}
            </Text>
            <Text style={styles.joinButtonSubtext}>지금 바로 입장하기</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>

    </View>
  );
};

