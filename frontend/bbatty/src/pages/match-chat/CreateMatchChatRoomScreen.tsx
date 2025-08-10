import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
  SafeAreaView,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Modal,
  FlatList,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { chatRoomApi } from '../../entities/chat-room/api/api';
import { gameApi } from '../../entities/game';
import type { CreateMatchChatRoomRequest } from '../../entities/chat-room/api/types';
import type { Game } from '../../entities/game';
import type { ChatStackParamList } from '../../navigation/types';

type NavigationProp = StackNavigationProp<ChatStackParamList>;

const TEAMS = [
  'LG', 'KT', 'SSG', 'NC', '두산', '기아', 'KIA', 'SK', '삼성', '롯데', '한화'
];

// 팀 이름을 ID로 매핑
const TEAM_ID_MAP: { [key: string]: number } = {
  'LG': 1,
  'KT': 2, 
  'SSG': 3,
  'NC': 4,
  '두산': 5,
  '기아': 6,
  'KIA': 6, // 기아와 KIA는 같은 팀
  'SK': 7,
  '삼성': 8,
  '롯데': 9,
  '한화': 10
};

const GENDER_OPTIONS = [
  { value: 'ALL', label: '전체' },
  { value: 'MALE', label: '남성만' },
  { value: 'FEMALE', label: '여성만' },
];

export const CreateMatchChatRoomScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const [loading, setLoading] = useState(false);
  const [gamesLoading, setGamesLoading] = useState(false);
  const [showGameModal, setShowGameModal] = useState(false);
  const [games, setGames] = useState<Game[]>([]);
  const [selectedGame, setSelectedGame] = useState<Game | null>(null);
  
  const [formData, setFormData] = useState<CreateMatchChatRoomRequest>({
    gameId: 0, // 경기 선택 후 설정
    matchTitle: '',
    matchDescription: '',
    teamId: 1, // LG 팀 ID (실제 서버 팀 ID로 매핑 필요)
    minAge: 20,
    maxAge: 30,
    genderCondition: 'ALL',
    maxParticipants: 10,
    nickname: '',
  });

  useEffect(() => {
    loadGames();
  }, []);

  const loadGames = async () => {
    try {
      setGamesLoading(true);
      const response = await gameApi.getGames();
      if (response.status === 'SUCCESS') {
        setGames(response.data);
      }
    } catch (error) {
      console.error('경기 목록 로드 실패:', error);
    } finally {
      setGamesLoading(false);
    }
  };

  const handleGameSelect = (game: Game) => {
    setSelectedGame(game);
    setFormData(prev => ({ ...prev, gameId: game.gameId.toString() }));
    setShowGameModal(false);
  };

  const getGameStatusText = (status: string) => {
    switch (status) {
      case 'LIVE': return '경기중';
      case 'SCHEDULED': return '경기예정';
      case 'FINISHED': return '경기종료';
      case 'POSTPONED': return '경기연기';
      case 'CANCELLED': return '경기취소';
      default: return status;
    }
  };

  const getGameStatusColor = (status: string) => {
    switch (status) {
      case 'LIVE': return '#4CAF50';
      case 'SCHEDULED': return '#2196F3';
      case 'FINISHED': return '#9E9E9E';
      case 'POSTPONED': return '#FF9800';
      case 'CANCELLED': return '#F44336';
      default: return '#9E9E9E';
    }
  };

  const handleSubmit = async () => {
    if (!selectedGame) {
      Alert.alert('알림', '경기를 먼저 선택해주세요.');
      return;
    }

    if (!formData.matchTitle.trim()) {
      Alert.alert('알림', '채팅방 제목을 입력해주세요.');
      return;
    }

    if (!formData.matchDescription.trim()) {
      Alert.alert('알림', '채팅방 설명을 입력해주세요.');
      return;
    }

    if (!formData.nickname.trim()) {
      Alert.alert('알림', '닉네임을 입력해주세요.');
      return;
    }

    if (formData.minAge >= formData.maxAge) {
      Alert.alert('알림', '최대 나이는 최소 나이보다 커야 합니다.');
      return;
    }

    try {
      setLoading(true);
      const response = await chatRoomApi.createMatchChatRoom(formData);
      
      console.log('📱 CreateMatchChatRoom 화면에서 받은 응답:', response);
      
      if (response.data?.status === 'SUCCESS') {
        Alert.alert(
          '성공',
          '채팅방이 생성되었습니다!',
          [
            {
              text: '확인',
              onPress: () => {
                if (navigation.canGoBack()) {
                  navigation.goBack();
                }
                // 필요시 생성된 방으로 바로 이동
                // navigation.navigate('MatchChatRoomDetail', { room: response.data.data });
              },
            },
          ]
        );
      } else {
        Alert.alert('오류', response.data?.message || '채팅방 생성에 실패했습니다.');
      }
    } catch (error) {
      console.error('채팅방 생성 실패:', error);
      Alert.alert('오류', '채팅방 생성에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const updateFormData = (key: keyof CreateMatchChatRoomRequest, value: any) => {
    if (key === 'teamId' && typeof value === 'string') {
      // 팀 이름을 ID로 변환
      const teamId = TEAM_ID_MAP[value];
      setFormData(prev => ({ ...prev, [key]: teamId || 1 }));
    } else {
      setFormData(prev => ({ ...prev, [key]: value }));
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView 
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <View style={styles.header}>
          <TouchableOpacity onPress={() => {
            if (navigation.canGoBack()) {
              navigation.goBack();
            }
          }}>
            <Text style={styles.cancelButton}>취소</Text>
          </TouchableOpacity>
          <Text style={styles.headerTitle}>채팅방 만들기</Text>
          <TouchableOpacity onPress={handleSubmit} disabled={loading}>
            <Text style={[styles.createButton, loading && styles.disabledButton]}>
              {loading ? '생성중...' : '완료'}
            </Text>
          </TouchableOpacity>
        </View>

        <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
          <View style={styles.section}>
            <Text style={styles.label}>경기 선택 *</Text>
            <TouchableOpacity
              style={[styles.gameSelectButton, !selectedGame && styles.gameSelectButtonEmpty]}
              onPress={() => setShowGameModal(true)}
            >
              {selectedGame ? (
                <View style={styles.selectedGameContainer}>
                  <View style={styles.gameInfo}>
                    <Text style={styles.gameTeams}>
                      {selectedGame.awayTeamName} vs {selectedGame.homeTeamName}
                    </Text>
                    <Text style={styles.gameDetails}>
                      {new Date(selectedGame.dateTime).toLocaleString('ko-KR', {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })} | {selectedGame.stadium}
                    </Text>
                  </View>
                </View>
              ) : (
                <Text style={styles.gameSelectPlaceholder}>
                  {gamesLoading ? '경기 목록 불러오는 중...' : '경기를 선택하세요'}
                </Text>
              )}
            </TouchableOpacity>
          </View>

          <View style={styles.section}>
            <Text style={styles.label}>채팅방 제목 *</Text>
            <TextInput
              style={styles.textInput}
              value={formData.matchTitle}
              onChangeText={(text) => updateFormData('matchTitle', text)}
              placeholder="예: 20대 LG팬들 모여라!"
              maxLength={50}
            />
          </View>

          <View style={styles.section}>
            <Text style={styles.label}>채팅방 설명 *</Text>
            <TextInput
              style={[styles.textInput, styles.textArea]}
              value={formData.matchDescription}
              onChangeText={(text) => updateFormData('matchDescription', text)}
              placeholder="채팅방에 대한 간단한 설명을 입력해주세요"
              multiline
              maxLength={200}
            />
          </View>

          <View style={styles.section}>
            <Text style={styles.label}>응원팀</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.teamContainer}>
              {TEAMS.map((team) => (
                <TouchableOpacity
                  key={team}
                  style={[
                    styles.teamButton,
                    formData.teamId === TEAM_ID_MAP[team] && styles.selectedTeamButton
                  ]}
                  onPress={() => updateFormData('teamId', team)}
                >
                  <Text style={[
                    styles.teamButtonText,
                    formData.teamId === TEAM_ID_MAP[team] && styles.selectedTeamButtonText
                  ]}>
                    {team}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>

          <View style={styles.section}>
            <Text style={styles.label}>참여 조건</Text>
            <View style={styles.conditionRow}>
              <View style={styles.ageContainer}>
                <Text style={styles.subLabel}>나이</Text>
                <View style={styles.ageInputContainer}>
                  <TextInput
                    style={styles.ageInput}
                    value={formData.minAge.toString()}
                    onChangeText={(text) => updateFormData('minAge', parseInt(text) || 0)}
                    keyboardType="numeric"
                    maxLength={2}
                  />
                  <Text style={styles.ageText}>-</Text>
                  <TextInput
                    style={styles.ageInput}
                    value={formData.maxAge.toString()}
                    onChangeText={(text) => updateFormData('maxAge', parseInt(text) || 0)}
                    keyboardType="numeric"
                    maxLength={2}
                  />
                  <Text style={styles.ageText}>세</Text>
                </View>
              </View>
            </View>

            <View style={styles.genderContainer}>
              <Text style={styles.subLabel}>성별</Text>
              <View style={styles.genderButtons}>
                {GENDER_OPTIONS.map((option) => (
                  <TouchableOpacity
                    key={option.value}
                    style={[
                      styles.genderButton,
                      formData.genderCondition === option.value && styles.selectedGenderButton
                    ]}
                    onPress={() => updateFormData('genderCondition', option.value)}
                  >
                    <Text style={[
                      styles.genderButtonText,
                      formData.genderCondition === option.value && styles.selectedGenderButtonText
                    ]}>
                      {option.label}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>
          </View>

          <View style={styles.section}>
            <Text style={styles.label}>닉네임 *</Text>
            <TextInput
              style={styles.textInput}
              value={formData.nickname}
              onChangeText={(text) => updateFormData('nickname', text)}
              placeholder="채팅방에서 사용할 닉네임을 입력하세요"
              maxLength={20}
            />
          </View>

          <View style={styles.section}>
            <Text style={styles.label}>최대 참여 인원</Text>
            <View style={styles.participantContainer}>
              <TouchableOpacity
                style={styles.participantButton}
                onPress={() => updateFormData('maxParticipants', Math.max(2, formData.maxParticipants - 1))}
              >
                <Text style={styles.participantButtonText}>-</Text>
              </TouchableOpacity>
              <Text style={styles.participantCount}>{formData.maxParticipants}명</Text>
              <TouchableOpacity
                style={styles.participantButton}
                onPress={() => updateFormData('maxParticipants', Math.min(50, formData.maxParticipants + 1))}
              >
                <Text style={styles.participantButtonText}>+</Text>
              </TouchableOpacity>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>

      {/* 경기 선택 모달 */}
      <Modal
        visible={showGameModal}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={() => setShowGameModal(false)}
      >
        <SafeAreaView style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <TouchableOpacity onPress={() => setShowGameModal(false)}>
              <Text style={styles.modalCancelButton}>취소</Text>
            </TouchableOpacity>
            <Text style={styles.modalTitle}>경기 선택</Text>
            <View style={styles.placeholder} />
          </View>

          <FlatList
            data={games}
            keyExtractor={(item) => item.gameId.toString()}
            renderItem={({ item: game }) => (
              <TouchableOpacity
                style={[
                  styles.gameItem,
                  selectedGame?.gameId === game.gameId && styles.selectedGameItem
                ]}
                onPress={() => handleGameSelect(game)}
              >
                <View style={styles.gameItemContent}>
                  <View style={styles.gameTeamsContainer}>
                    <Text style={styles.gameItemTeams}>
                      {game.awayTeamName} vs {game.homeTeamName}
                    </Text>
                  </View>
                  
                  <Text style={styles.gameItemDetails}>
                    {new Date(game.dateTime).toLocaleString('ko-KR', {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })} | {game.stadium}
                  </Text>
                </View>
              </TouchableOpacity>
            )}
            showsVerticalScrollIndicator={false}
          />
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
  cancelButton: {
    color: '#666',
    fontSize: 16,
  },
  createButton: {
    color: '#007AFF',
    fontSize: 16,
    fontWeight: '600',
  },
  disabledButton: {
    color: '#ccc',
  },
  content: {
    flex: 1,
  },
  section: {
    backgroundColor: '#fff',
    marginTop: 12,
    paddingHorizontal: 16,
    paddingVertical: 16,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 12,
  },
  subLabel: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  textInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
    backgroundColor: '#f9f9f9',
  },
  textArea: {
    height: 80,
    textAlignVertical: 'top',
  },
  teamContainer: {
    marginTop: 8,
  },
  teamButton: {
    backgroundColor: '#f0f0f0',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    marginRight: 8,
  },
  selectedTeamButton: {
    backgroundColor: '#007AFF',
  },
  teamButtonText: {
    fontSize: 14,
    color: '#666',
  },
  selectedTeamButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
  conditionRow: {
    marginBottom: 16,
  },
  ageContainer: {
    marginBottom: 16,
  },
  ageInputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  ageInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontSize: 16,
    width: 60,
    textAlign: 'center',
    backgroundColor: '#f9f9f9',
  },
  ageText: {
    marginHorizontal: 8,
    fontSize: 16,
    color: '#666',
  },
  genderContainer: {
    marginBottom: 16,
  },
  genderButtons: {
    flexDirection: 'row',
  },
  genderButton: {
    backgroundColor: '#f0f0f0',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    marginRight: 8,
  },
  selectedGenderButton: {
    backgroundColor: '#007AFF',
  },
  genderButtonText: {
    fontSize: 14,
    color: '#666',
  },
  selectedGenderButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
  participantContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 12,
  },
  participantButton: {
    backgroundColor: '#007AFF',
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
  participantButtonText: {
    color: '#fff',
    fontSize: 20,
    fontWeight: 'bold',
  },
  participantCount: {
    marginHorizontal: 20,
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  // 경기 선택 관련 스타일
  gameSelectButton: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 16,
    backgroundColor: '#f9f9f9',
  },
  gameSelectButtonEmpty: {
    borderColor: '#007AFF',
    borderStyle: 'dashed',
  },
  selectedGameContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  gameInfo: {
    flex: 1,
  },
  gameTeams: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  gameDetails: {
    fontSize: 12,
    color: '#666',
  },
  gameStatusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  gameStatusText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: '600',
  },
  gameSelectPlaceholder: {
    fontSize: 16,
    color: '#999',
    textAlign: 'center',
  },
  // 모달 스타일
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
  placeholder: {
    width: 40,
  },
  dateGroup: {
    marginBottom: 16,
  },
  dateHeader: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  gameItem: {
    backgroundColor: '#fff',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  selectedGameItem: {
    backgroundColor: '#E3F2FD',
  },
  gameItemContent: {
    flex: 1,
  },
  gameTeamsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  gameItemTeams: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    flex: 1,
  },
  gameItemDetails: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
  },
  activeUserCount: {
    fontSize: 12,
    color: '#007AFF',
    fontWeight: '500',
  },
});