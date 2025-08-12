import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  SafeAreaView,
  Alert,
} from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import type { RouteProp } from '@react-navigation/native';
import { chatRoomApi } from '../../entities/chat-room/api/api';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
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
  
  const [joining, setJoining] = useState(false);

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
        winRate: 75, // TODO: 실제 승률 데이터 연결
        profileImgUrl: currentUser.profileImageURL || 'https://example.com/profile.jpg',
        isWinFairy: false, // TODO: 실제 승부요정 여부 연결
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

  const getTeamColors = (teamId: string) => {
    const teamColorMap: { [key: string]: string[] } = {
      'LG': ['#C30452', '#8B0000'],
      '두산': ['#131230', '#000080'],
      'KIA': ['#EA0029', '#DC143C'],
      '삼성': ['#074CA1', '#0066CC'],
      '롯데': ['#041E42', '#000080'],
      'SSG': ['#CE0E2D', '#B22222'],
      'KT': ['#000000', '#2F2F2F'],
      '한화': ['#FF6600', '#FF4500'],
      'NC': ['#315288', '#4169E1'],
      '키움': ['#570514', '#8B0000'],
    };
    return teamColorMap[teamId] || ['#007AFF', '#0066CC'];
  };

  const teamColors = getTeamColors(room.teamId);

  return (
    <SafeAreaView style={styles.container}>
      <View style={[styles.header, { backgroundColor: themeColor }]}>
        <TouchableOpacity onPress={() => {
          if (navigation.canGoBack()) {
            navigation.goBack();
          }
        }}>
          <Text style={[styles.backButton, { color: '#ffffff' }]}>← 뒤로</Text>
        </TouchableOpacity>
        <Text style={[styles.headerTitle, { color: '#ffffff' }]}>매치룸 정보</Text>
        <View style={styles.placeholder} />
      </View>

      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.roomCard}>
          <View style={[styles.cardGradient, { backgroundColor: '#FFFFFF' }]}>
            <View style={styles.roomHeader}>
              <View style={styles.titleContainer}>
                <Text style={styles.roomTitle}>{room.matchTitle}</Text>
                <Text style={styles.participantCount}>
                  {room.currentParticipants}/{room.maxParticipants} 선수
                </Text>
              </View>
              <View style={[styles.teamBadge, { backgroundColor: teamColors[0] }]}>
                <Text style={styles.teamText}>{room.teamId}</Text>
              </View>
            </View>
            
            <Text style={styles.roomDescription}>{room.matchDescription}</Text>
            
            <View style={styles.statusContainer}>
              <View style={[
                styles.statusBadge,
                isRoomActive ? styles.activeBadge : styles.inactiveBadge
              ]}>
                <Text style={[
                  styles.statusText,
                  isRoomActive ? styles.activeText : styles.inactiveText
                ]}>
                  {isRoomActive ? '경기중' : '대기중'}
                </Text>
              </View>
              
              {isRoomFull && (
                <View style={styles.fullBadge}>
                  <Text style={styles.fullText}>만루</Text>
                </View>
              )}
            </View>
          </View>
        </View>

        <View style={styles.infoSection}>
          <Text style={styles.sectionTitle}>엔트리 조건</Text>
          
          <View style={styles.infoGrid}>
            <View style={styles.infoCard}>
              <Text style={styles.infoCardLabel}>선수단</Text>
              <Text style={styles.infoCardValue}>
                {room.currentParticipants}/{room.maxParticipants}명
              </Text>
            </View>
            
            <View style={styles.infoCard}>
              <Text style={styles.infoCardLabel}>연령대</Text>
              <Text style={styles.infoCardValue}>{room.minAge}-{room.maxAge}세</Text>
            </View>
          </View>

          <View style={styles.infoGrid}>
            <View style={styles.infoCard}>
              <Text style={styles.infoCardLabel}>참가 조건</Text>
              <Text style={styles.infoCardValue}>{getGenderText(room.genderCondition)}</Text>
            </View>
            
            <View style={styles.infoCard}>
              <Text style={styles.infoCardLabel}>개설일</Text>
              <Text style={styles.infoCardValue}>
                {new Date(room.createdAt).toLocaleDateString('ko-KR')}
              </Text>
            </View>
          </View>
          
          {room.gameId && (
            <View style={styles.gameIdCard}>
              <View style={styles.gameIdInfo}>
                <Text style={styles.gameIdLabel}>경기 정보</Text>
                <Text style={styles.gameIdValue}>{room.gameId}</Text>
              </View>
            </View>
          )}
        </View>

        <View style={styles.actionSection}>
          {(!isRoomActive || isRoomFull) ? (
            <View style={styles.joinButtonDisabled}>
              <Text style={styles.joinButtonTextDisabled}>
                {!isRoomActive ? '경기 대기중' : '만루 (인원 마감)'}
              </Text>
            </View>
          ) : (
            <TouchableOpacity
              style={[styles.joinButton, { backgroundColor: '#FF6B35' }]}
              onPress={handleJoinRoom}
              disabled={joining}
              activeOpacity={0.8}
            >
              <Text style={styles.joinButtonText}>
                {joining ? '참여중...' : '경기 참여하기'}
              </Text>
              <Text style={styles.joinButtonSubtext}>지금 바로 선수 등록</Text>
            </TouchableOpacity>
          )}
        </View>
      </ScrollView>

    </SafeAreaView>
  );
};

