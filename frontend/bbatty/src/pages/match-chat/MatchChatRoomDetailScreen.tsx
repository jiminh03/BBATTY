import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
  SafeAreaView,
  Alert,
  Modal,
  TextInput,
} from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import type { RouteProp } from '@react-navigation/native';
import { chatRoomApi } from '../../entities/chat-room/api/api';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { ChatStackParamList } from '../../navigation/types';

type NavigationProp = StackNavigationProp<ChatStackParamList>;
type RoutePropType = RouteProp<ChatStackParamList, 'MatchChatRoomDetail'>;

export const MatchChatRoomDetailScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<RoutePropType>();
  const { room } = route.params;
  
  const [joining, setJoining] = useState(false);
  const [showJoinModal, setShowJoinModal] = useState(false);
  const [nickname, setNickname] = useState('');
  const [winRate, setWinRate] = useState('75');

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
    if (!nickname.trim()) {
      Alert.alert('알림', '닉네임을 입력해주세요.');
      return;
    }

    if (!winRate || parseInt(winRate) < 0 || parseInt(winRate) > 100) {
      Alert.alert('알림', '승률은 0-100 사이의 값을 입력해주세요.');
      return;
    }

    try {
      setJoining(true);
      
      const joinRequest = {
        matchId: room.matchId,
        nickname: nickname.trim(),
        winRate: parseInt(winRate),
        profileImgUrl: 'https://example.com/profile.jpg', // 기본값
        isVictoryFairy: false, // 기본값
      };

      const response = await chatRoomApi.joinMatchChat(joinRequest);
      
      setShowJoinModal(false);
      
      // 채팅방으로 이동
      if (response.data.status === 'SUCCESS') {
        navigation.navigate('MatchChatRoom', { 
          room,
          websocketUrl: response.data.data.websocketUrl,
          sessionToken: response.data.data.sessionToken
        });
      } else {
        Alert.alert('오류', response.data.message || '채팅방 참여에 실패했습니다.');
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
      <View style={styles.header}>
        <TouchableOpacity onPress={() => {
          if (navigation.canGoBack()) {
            navigation.goBack();
          }
        }}>
          <Text style={styles.backButton}>← 뒤로</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>⚾ 매치룸 정보</Text>
        <View style={styles.placeholder} />
      </View>

      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.roomCard}>
          <View style={[styles.cardGradient, { backgroundColor: '#FFFFFF' }]}>
            <View style={styles.roomHeader}>
              <View style={styles.titleContainer}>
                <Text style={styles.roomTitle}>🔥 {room.matchTitle}</Text>
                <Text style={styles.participantCount}>
                  👥 {room.currentParticipants}/{room.maxParticipants} 선수
                </Text>
              </View>
              <View style={[styles.teamBadge, { backgroundColor: teamColors[0] }]}>
                <Text style={styles.teamText}>⚾ {room.teamId}</Text>
              </View>
            </View>
            
            <Text style={styles.roomDescription}>🏟️ {room.matchDescription}</Text>
            
            <View style={styles.statusContainer}>
              <View style={[
                styles.statusBadge,
                isRoomActive ? styles.activeBadge : styles.inactiveBadge
              ]}>
                <Text style={[
                  styles.statusText,
                  isRoomActive ? styles.activeText : styles.inactiveText
                ]}>
                  {isRoomActive ? '⚡ 경기중' : '💤 대기중'}
                </Text>
              </View>
              
              {isRoomFull && (
                <View style={styles.fullBadge}>
                  <Text style={styles.fullText}>🚨 만루</Text>
                </View>
              )}
            </View>
          </View>
        </View>

        <View style={styles.infoSection}>
          <Text style={styles.sectionTitle}>📋 엔트리 조건</Text>
          
          <View style={styles.infoGrid}>
            <View style={styles.infoCard}>
              <Text style={styles.infoIcon}>👥</Text>
              <Text style={styles.infoCardLabel}>선수단</Text>
              <Text style={styles.infoCardValue}>
                {room.currentParticipants}/{room.maxParticipants}명
              </Text>
            </View>
            
            <View style={styles.infoCard}>
              <Text style={styles.infoIcon}>🎂</Text>
              <Text style={styles.infoCardLabel}>연령대</Text>
              <Text style={styles.infoCardValue}>{room.minAge}-{room.maxAge}세</Text>
            </View>
          </View>

          <View style={styles.infoGrid}>
            <View style={styles.infoCard}>
              <Text style={styles.infoIcon}>⚡</Text>
              <Text style={styles.infoCardLabel}>참가 조건</Text>
              <Text style={styles.infoCardValue}>{getGenderText(room.genderCondition)}</Text>
            </View>
            
            <View style={styles.infoCard}>
              <Text style={styles.infoIcon}>📅</Text>
              <Text style={styles.infoCardLabel}>개설일</Text>
              <Text style={styles.infoCardValue}>
                {new Date(room.createdAt).toLocaleDateString('ko-KR')}
              </Text>
            </View>
          </View>
          
          {room.gameId && (
            <View style={styles.gameIdCard}>
              <Text style={styles.gameIdIcon}>🏟️</Text>
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
                {!isRoomActive ? '⏸️ 경기 대기중' : '🚨 만루 (인원 마감)'}
              </Text>
            </View>
          ) : (
            <TouchableOpacity
              style={[styles.joinButton, { backgroundColor: '#FF6B35' }]}
              onPress={() => setShowJoinModal(true)}
              activeOpacity={0.8}
            >
              <Text style={styles.joinButtonText}>⚾ 경기 참여하기! 🔥</Text>
              <Text style={styles.joinButtonSubtext}>지금 바로 선수 등록</Text>
            </TouchableOpacity>
          )}
        </View>
      </ScrollView>

      {/* 입장 모달 */}
      <Modal
        visible={showJoinModal}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setShowJoinModal(false)}
      >
        <SafeAreaView style={styles.modalContainer}>
          <View style={[styles.modalHeader, { backgroundColor: '#2E7D32' }]}>
            <TouchableOpacity onPress={() => setShowJoinModal(false)}>
              <Text style={styles.modalCancelButton}>✖️ 취소</Text>
            </TouchableOpacity>
            <Text style={styles.modalTitle}>⚾ 선수 등록</Text>
            <TouchableOpacity onPress={handleJoinRoom} disabled={joining}>
              <Text style={[styles.modalJoinButton, joining && styles.disabledButton]}>
                {joining ? '⏳ 등록중...' : '🔥 등록'}
              </Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalContent}>
            <View style={styles.playerCardContainer}>
              <View style={[styles.playerCard, { backgroundColor: teamColors[0] }]}>
                <Text style={styles.playerCardTitle}>🏆 선수 카드</Text>
                <Text style={styles.playerCardSubtitle}>{room.teamId} 팬클럽</Text>
              </View>
            </View>

            <View style={styles.modalSection}>
              <Text style={styles.modalLabel}>⚾ 선수명 (닉네임) *</Text>
              <TextInput
                style={styles.modalInput}
                value={nickname}
                onChangeText={setNickname}
                placeholder="경기에서 사용할 선수명을 입력하세요"
                maxLength={20}
              />
            </View>

            <View style={styles.modalSection}>
              <Text style={styles.modalLabel}>📊 시즌 승률 (%)</Text>
              <TextInput
                style={styles.modalInput}
                value={winRate}
                onChangeText={setWinRate}
                placeholder="승률을 입력하세요 (0-100)"
                keyboardType="numeric"
                maxLength={3}
              />
              <Text style={styles.modalHint}>
                🏅 승률은 다른 선수들에게 표시되어 팀 전력을 보여줍니다.
              </Text>
            </View>

            <View style={styles.modalRoomInfo}>
              <Text style={styles.modalRoomTitle}>🔥 {room.matchTitle}</Text>
              <Text style={styles.modalRoomDescription}>🏟️ {room.matchDescription}</Text>
            </View>
          </ScrollView>
        </SafeAreaView>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F0F8F0',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#2E7D32',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.2,
    shadowRadius: 6,
    elevation: 8,
  },
  headerTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#FFFFFF',
    textShadowColor: 'rgba(0, 0, 0, 0.3)',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 2,
  },
  backButton: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '600',
    textShadowColor: 'rgba(0, 0, 0, 0.3)',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 2,
  },
  placeholder: {
    width: 50,
  },
  content: {
    flex: 1,
  },
  roomCard: {
    margin: 20,
    borderRadius: 20,
    transform: [{ rotate: '-1deg' }],
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 8,
    },
    shadowOpacity: 0.25,
    shadowRadius: 12,
    elevation: 15,
  },
  cardGradient: {
    borderRadius: 20,
    padding: 24,
  },
  roomHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 16,
  },
  titleContainer: {
    flex: 1,
    marginRight: 16,
  },
  roomTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1B5E20',
    marginBottom: 8,
    textShadowColor: 'rgba(0, 0, 0, 0.1)',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 2,
  },
  participantCount: {
    fontSize: 16,
    fontWeight: '600',
    color: '#FF6B35',
  },
  teamBadge: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 6,
    elevation: 8,
  },
  teamText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
    textShadowColor: 'rgba(0, 0, 0, 0.3)',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 2,
  },
  roomDescription: {
    fontSize: 18,
    color: '#2E7D32',
    lineHeight: 26,
    marginBottom: 20,
    fontStyle: 'italic',
  },
  statusContainer: {
    flexDirection: 'row',
    gap: 12,
    alignItems: 'center',
  },
  statusBadge: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 4,
  },
  activeBadge: {
    backgroundColor: '#4CAF50',
  },
  inactiveBadge: {
    backgroundColor: '#FF5722',
  },
  statusText: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  activeText: {
    color: '#FFFFFF',
  },
  inactiveText: {
    color: '#FFFFFF',
  },
  fullBadge: {
    backgroundColor: '#FF9800',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 4,
  },
  fullText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: 'bold',
  },
  infoSection: {
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    marginHorizontal: 20,
    marginBottom: 20,
    borderRadius: 20,
    padding: 24,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 6,
    },
    shadowOpacity: 0.15,
    shadowRadius: 8,
    elevation: 10,
  },
  sectionTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#1B5E20',
    marginBottom: 20,
    textAlign: 'center',
  },
  infoGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  infoCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 16,
    width: '47%',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 6,
  },
  infoIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  infoCardLabel: {
    fontSize: 12,
    color: '#666',
    fontWeight: '500',
    marginBottom: 4,
  },
  infoCardValue: {
    fontSize: 16,
    color: '#1B5E20',
    fontWeight: 'bold',
  },
  gameIdCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 16,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 6,
  },
  gameIdIcon: {
    fontSize: 24,
    marginRight: 12,
  },
  gameIdInfo: {
    flex: 1,
  },
  gameIdLabel: {
    fontSize: 12,
    color: '#666',
    fontWeight: '500',
    marginBottom: 4,
  },
  gameIdValue: {
    fontSize: 16,
    color: '#1B5E20',
    fontWeight: 'bold',
  },
  actionSection: {
    paddingHorizontal: 24,
    paddingBottom: 40,
    paddingTop: 8,
  },
  joinButton: {
    borderRadius: 25,
    paddingVertical: 18,
    paddingHorizontal: 24,
    alignItems: 'center',
    shadowColor: '#FF6B35',
    shadowOffset: {
      width: 0,
      height: 8,
    },
    shadowOpacity: 0.4,
    shadowRadius: 12,
    elevation: 15,
  },
  joinButtonDisabled: {
    backgroundColor: '#BDBDBD',
    borderRadius: 25,
    paddingVertical: 18,
    alignItems: 'center',
  },
  joinButtonText: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: 'bold',
    textShadowColor: 'rgba(0, 0, 0, 0.3)',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 2,
    marginBottom: 4,
  },
  joinButtonSubtext: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '500',
    opacity: 0.9,
  },
  joinButtonTextDisabled: {
    color: '#757575',
    fontSize: 18,
    fontWeight: 'bold',
  },
  // Modal styles
  modalContainer: {
    flex: 1,
    backgroundColor: '#F0F8F0',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.2,
    shadowRadius: 6,
    elevation: 8,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#FFFFFF',
    textShadowColor: 'rgba(0, 0, 0, 0.3)',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 2,
  },
  modalCancelButton: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  modalJoinButton: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
  disabledButton: {
    color: 'rgba(255, 255, 255, 0.5)',
  },
  playerCardContainer: {
    alignItems: 'center',
    marginBottom: 24,
  },
  playerCard: {
    borderRadius: 20,
    paddingHorizontal: 40,
    paddingVertical: 20,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 6,
    },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 12,
  },
  playerCardTitle: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 4,
  },
  playerCardSubtitle: {
    color: '#FFFFFF',
    fontSize: 14,
    opacity: 0.9,
  },
  modalContent: {
    flex: 1,
    padding: 20,
  },
  modalSection: {
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderRadius: 16,
    padding: 20,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 6,
  },
  modalLabel: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1B5E20',
    marginBottom: 12,
  },
  modalInput: {
    borderWidth: 2,
    borderColor: '#4CAF50',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    fontSize: 16,
    backgroundColor: '#FFFFFF',
  },
  modalHint: {
    fontSize: 14,
    color: '#2E7D32',
    marginTop: 12,
    lineHeight: 20,
    fontStyle: 'italic',
  },
  modalRoomInfo: {
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderRadius: 16,
    padding: 20,
    borderLeftWidth: 6,
    borderLeftColor: '#FF6B35',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 6,
  },
  modalRoomTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1B5E20',
    marginBottom: 8,
  },
  modalRoomDescription: {
    fontSize: 16,
    color: '#2E7D32',
    lineHeight: 24,
    fontStyle: 'italic',
  },
});