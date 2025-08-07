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

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.backButton}>← 뒤로</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>채팅방 정보</Text>
        <View style={styles.placeholder} />
      </View>

      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.roomCard}>
          <View style={styles.roomHeader}>
            <Text style={styles.roomTitle}>{room.matchTitle}</Text>
            <View style={styles.teamBadge}>
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
                {isRoomActive ? '활성' : '비활성'}
              </Text>
            </View>
            
            {isRoomFull && (
              <View style={styles.fullBadge}>
                <Text style={styles.fullText}>만석</Text>
              </View>
            )}
          </View>
        </View>

        <View style={styles.infoSection}>
          <Text style={styles.sectionTitle}>참여 정보</Text>
          
          <View style={styles.infoItem}>
            <Text style={styles.infoLabel}>현재 참여자</Text>
            <Text style={styles.infoValue}>
              {room.currentParticipants}/{room.maxParticipants}명
            </Text>
          </View>
          
          <View style={styles.infoItem}>
            <Text style={styles.infoLabel}>참여 나이</Text>
            <Text style={styles.infoValue}>{room.minAge}세 - {room.maxAge}세</Text>
          </View>
          
          <View style={styles.infoItem}>
            <Text style={styles.infoLabel}>성별 조건</Text>
            <Text style={styles.infoValue}>{getGenderText(room.genderCondition)}</Text>
          </View>
          
          <View style={styles.infoItem}>
            <Text style={styles.infoLabel}>생성일시</Text>
            <Text style={styles.infoValue}>{formatDate(room.createdAt)}</Text>
          </View>
          
          {room.gameId && (
            <View style={styles.infoItem}>
              <Text style={styles.infoLabel}>경기 ID</Text>
              <Text style={styles.infoValue}>{room.gameId}</Text>
            </View>
          )}
        </View>

        <View style={styles.actionSection}>
          <TouchableOpacity
            style={[
              styles.joinButton,
              (!isRoomActive || isRoomFull) && styles.joinButtonDisabled
            ]}
            onPress={() => setShowJoinModal(true)}
            disabled={!isRoomActive || isRoomFull}
          >
            <Text style={[
              styles.joinButtonText,
              (!isRoomActive || isRoomFull) && styles.joinButtonTextDisabled
            ]}>
              {!isRoomActive ? '비활성 채팅방' : 
               isRoomFull ? '인원 마감' : '채팅방 입장하기'}
            </Text>
          </TouchableOpacity>
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
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setShowJoinModal(false)}>
              <Text style={styles.modalCancelButton}>취소</Text>
            </TouchableOpacity>
            <Text style={styles.modalTitle}>채팅방 입장</Text>
            <TouchableOpacity onPress={handleJoinRoom} disabled={joining}>
              <Text style={[styles.modalJoinButton, joining && styles.disabledButton]}>
                {joining ? '입장중...' : '입장'}
              </Text>
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.modalContent}>
            <View style={styles.modalSection}>
              <Text style={styles.modalLabel}>닉네임 *</Text>
              <TextInput
                style={styles.modalInput}
                value={nickname}
                onChangeText={setNickname}
                placeholder="채팅에서 사용할 닉네임을 입력하세요"
                maxLength={20}
              />
            </View>

            <View style={styles.modalSection}>
              <Text style={styles.modalLabel}>승률 (%)</Text>
              <TextInput
                style={styles.modalInput}
                value={winRate}
                onChangeText={setWinRate}
                placeholder="승률을 입력하세요 (0-100)"
                keyboardType="numeric"
                maxLength={3}
              />
              <Text style={styles.modalHint}>
                승률은 선택사항이며, 다른 참여자들에게 표시됩니다.
              </Text>
            </View>

            <View style={styles.modalRoomInfo}>
              <Text style={styles.modalRoomTitle}>{room.matchTitle}</Text>
              <Text style={styles.modalRoomDescription}>{room.matchDescription}</Text>
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
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  backButton: {
    color: '#007AFF',
    fontSize: 16,
  },
  placeholder: {
    width: 40,
  },
  content: {
    flex: 1,
  },
  roomCard: {
    backgroundColor: '#fff',
    margin: 16,
    borderRadius: 12,
    padding: 20,
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
    marginBottom: 12,
  },
  roomTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    flex: 1,
    marginRight: 12,
  },
  teamBadge: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 12,
  },
  teamText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  roomDescription: {
    fontSize: 16,
    color: '#666',
    lineHeight: 24,
    marginBottom: 16,
  },
  statusContainer: {
    flexDirection: 'row',
    gap: 8,
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
  },
  activeBadge: {
    backgroundColor: '#E8F5E8',
  },
  inactiveBadge: {
    backgroundColor: '#FFEBEE',
  },
  statusText: {
    fontSize: 12,
    fontWeight: '600',
  },
  activeText: {
    color: '#4CAF50',
  },
  inactiveText: {
    color: '#F44336',
  },
  fullBadge: {
    backgroundColor: '#FFF3E0',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
  },
  fullText: {
    color: '#FF9800',
    fontSize: 12,
    fontWeight: '600',
  },
  infoSection: {
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginBottom: 16,
    borderRadius: 12,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 16,
  },
  infoItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  infoLabel: {
    fontSize: 14,
    color: '#666',
  },
  infoValue: {
    fontSize: 14,
    color: '#333',
    fontWeight: '500',
  },
  actionSection: {
    padding: 16,
    paddingBottom: 32,
  },
  joinButton: {
    backgroundColor: '#007AFF',
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
  },
  joinButtonDisabled: {
    backgroundColor: '#ccc',
  },
  joinButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  joinButtonTextDisabled: {
    color: '#999',
  },
  // Modal styles
  modalContainer: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  modalCancelButton: {
    color: '#666',
    fontSize: 16,
  },
  modalJoinButton: {
    color: '#007AFF',
    fontSize: 16,
    fontWeight: '600',
  },
  disabledButton: {
    color: '#ccc',
  },
  modalContent: {
    flex: 1,
    padding: 16,
  },
  modalSection: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
  },
  modalLabel: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  modalInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
    backgroundColor: '#f9f9f9',
  },
  modalHint: {
    fontSize: 12,
    color: '#999',
    marginTop: 8,
    lineHeight: 16,
  },
  modalRoomInfo: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
  },
  modalRoomTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
  },
  modalRoomDescription: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
  },
});