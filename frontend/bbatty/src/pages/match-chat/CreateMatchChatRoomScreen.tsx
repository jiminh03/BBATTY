import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Modal,
  FlatList,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { chatRoomApi } from '../../entities/chat-room/api/api';
import { gameApi } from '../../entities/game';
import type { CreateMatchChatRoomRequest } from '../../entities/chat-room/api/types';
import type { Game } from '../../entities/game';
import type { ChatStackParamList } from '../../navigation/types';
import { useThemeColor } from '../../shared/team/ThemeContext';
import { useUserStore } from '../../entities/user/model/userStore';
import { styles } from './CreateMatchChatRoomScreen.styles';

type NavigationProp = StackNavigationProp<ChatStackParamList>;

const TEAMS = ['LG', 'KT', 'SSG', 'NC', '두산', '기아', 'KIA', 'SK', '삼성', '롯데', '한화'];

// 팀 이름을 ID로 매핑 (실제 teamTypes.ts와 일치)
const TEAM_ID_MAP: { [key: string]: number } = {
  LG: 2,
  KT: 4,
  SSG: 7,
  NC: 8,
  두산: 9,
  기아: 6,
  KIA: 6,
  SK: 7,
  삼성: 5,
  롯데: 3,
  한화: 1,
};

// ID를 팀 이름으로 역매핑 (실제 teamTypes.ts와 일치)
const ID_TO_TEAM_NAME_MAP: { [key: number]: string[] } = {
  1: ['한화 이글스'],
  2: ['LG 트윈스'],
  3: ['롯데 자이언츠'],
  4: ['KT 위즈'],
  5: ['삼성 라이온즈'],
  6: ['KIA 타이거즈'],
  7: ['SSG 랜더스'],
  8: ['NC 다이노스'],
  9: ['두산 베어스'],
  10: ['키움 히어로즈'],
};

const GENDER_OPTIONS = [
  { value: 'ALL', label: '전체' },
  { value: 'MALE', label: '남성만' },
  { value: 'FEMALE', label: '여성만' },
];

export const CreateMatchChatRoomScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const themeColor = useThemeColor();
  const getCurrentUser = useUserStore((state) => state.getCurrentUser);
  const currentUser = getCurrentUser();
  const insets = useSafeAreaInsets();
  const [loading, setLoading] = useState(false);
  const [gamesLoading, setGamesLoading] = useState(false);
  const [showGameModal, setShowGameModal] = useState(false);
  const [games, setGames] = useState<Game[]>([]);
  const [selectedGame, setSelectedGame] = useState<Game | null>(null);

  const [formData, setFormData] = useState<CreateMatchChatRoomRequest>({
    gameId: 0, // 경기 선택 후 설정
    matchTitle: '',
    matchDescription: '',
    teamId: currentUser?.teamId || 1,
    minAge: 20,
    maxAge: 100,
    genderCondition: 'ALL',
    maxParticipants: 10,
    nickname: currentUser?.nickname || '',
  });

  useEffect(() => {
    loadGames();
  }, []);

  const loadGames = async () => {
    try {
      setGamesLoading(true);
      const response = await gameApi.getGames();
      if (response.status === 'SUCCESS') {
        // 내 팀의 경기만 필터링
        const myTeamGames = currentUser?.teamId
          ? response.data.filter((game) => {
              const myTeamNames = ID_TO_TEAM_NAME_MAP[currentUser.teamId] || [];
              return myTeamNames.some(
                (teamName) =>
                  game.awayTeamName.includes(teamName.split(' ')[0]) ||
                  game.homeTeamName.includes(teamName.split(' ')[0])
              );
            })
          : response.data;
        setGames(myTeamGames);
      } else {
      }
    } catch (error) {
    } finally {
      setGamesLoading(false);
    }
  };

  const handleGameSelect = (game: Game) => {
    setSelectedGame(game);
    setFormData((prev) => ({ ...prev, gameId: game.gameId.toString() }));
    setShowGameModal(false);
  };

  const getGameStatusText = (status: string) => {
    switch (status) {
      case 'LIVE':
        return '경기중';
      case 'SCHEDULED':
        return '경기예정';
      case 'FINISHED':
        return '경기종료';
      case 'POSTPONED':
        return '경기연기';
      case 'CANCELLED':
        return '경기취소';
      default:
        return status;
    }
  };

  const getGameStatusColor = (status: string) => {
    switch (status) {
      case 'LIVE':
        return '#4CAF50';
      case 'SCHEDULED':
        return '#2196F3';
      case 'FINISHED':
        return '#9E9E9E';
      case 'POSTPONED':
        return '#FF9800';
      case 'CANCELLED':
        return '#F44336';
      default:
        return '#9E9E9E';
    }
  };

  const checkUserConditions = () => {
    if (!currentUser) {
      Alert.alert('오류', '사용자 정보를 확인할 수 없습니다.');
      return false;
    }

    // 나이 조건 체크
    if (currentUser.age < formData.minAge || currentUser.age > formData.maxAge) {
      Alert.alert('참여 불가', `이 채팅방은 ${formData.minAge}세-${formData.maxAge}세만 참여할 수 있습니다.`);
      return false;
    }

    // 성별 조건 체크
    if (formData.genderCondition !== 'ALL') {
      if (formData.genderCondition === 'MALE' && currentUser.gender !== 'MALE') {
        Alert.alert('참여 불가', '이 채팅방은 남성만 참여할 수 있습니다.');
        return false;
      }
      if (formData.genderCondition === 'FEMALE' && currentUser.gender !== 'FEMALE') {
        Alert.alert('참여 불가', '이 채팅방은 여성만 참여할 수 있습니다.');
        return false;
      }
    }

    return true;
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

    // 나이 범위 강제 검증 및 수정 (20-100세)
    const correctedMinAge = Math.max(20, Math.min(100, formData.minAge || 20));
    const correctedMaxAge = Math.max(20, Math.min(100, formData.maxAge || 100));

    // 자동 조정 (사용자에게 알림 없이)
    if (formData.minAge !== correctedMinAge || formData.maxAge !== correctedMaxAge) {
      setFormData((prev) => ({
        ...prev,
        minAge: correctedMinAge,
        maxAge: correctedMaxAge,
      }));
    }

    if (formData.minAge >= formData.maxAge) {
      Alert.alert('알림', '최대 나이는 최소 나이보다 커야 합니다.');
      return;
    }

    // 사용자 조건 체크
    if (!checkUserConditions()) {
      return;
    }

    try {
      setLoading(true);
      const response = await chatRoomApi.createMatchChatRoom(formData);

      console.log('📱 CreateMatchChatRoom 화면에서 받은 응답:', response);

      if (response.data?.status === 'SUCCESS') {
        Alert.alert('성공', '채팅방이 생성되었습니다!', [
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
        ]);
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
      setFormData((prev) => ({ ...prev, [key]: teamId || 1 }));
    } else if (key === 'minAge' || key === 'maxAge') {
      // 나이 제한 적용 (20-100세)
      const numericValue = value.toString().replace(/[^0-9]/g, ''); // 숫자만 추출
      if (numericValue === '') {
        return; // 빈 문자열일 때는 업데이트하지 않음
      }
      const age = parseInt(numericValue);
      if (isNaN(age)) return;
      const clampedAge = Math.max(20, Math.min(100, age));
      setFormData((prev) => ({ ...prev, [key]: clampedAge }));
    } else {
      setFormData((prev) => ({ ...prev, [key]: value }));
    }
  };

  return (
    <View style={styles.container}>
      <LinearGradient colors={[themeColor, themeColor]} style={[styles.headerGradient, { paddingTop: insets.top }]}>
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => {
              if (navigation.canGoBack()) {
                navigation.goBack();
              }
            }}
          >
            <Text style={[styles.cancelButton, { color: '#ffffff' }]}>취소</Text>
          </TouchableOpacity>
          <Text style={[styles.headerTitle, { color: '#ffffff' }]}>채팅방 만들기</Text>
          <TouchableOpacity onPress={handleSubmit} disabled={loading}>
            <Text style={[styles.createButton, { color: '#ffffff' }, loading && styles.disabledButton]}>
              {loading ? '생성중...' : '완료'}
            </Text>
          </TouchableOpacity>
        </View>
      </LinearGradient>

      <KeyboardAvoidingView style={styles.keyboardContainer} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
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
                        minute: '2-digit',
                      })}{' '}
                      | {selectedGame.stadium}
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
              placeholder='예: 20대 LG팬들 모여라!'
              maxLength={50}
            />
          </View>

          <View style={styles.section}>
            <Text style={styles.label}>채팅방 설명 *</Text>
            <TextInput
              style={[styles.textInput, styles.textArea]}
              value={formData.matchDescription}
              onChangeText={(text) => updateFormData('matchDescription', text)}
              placeholder='채팅방에 대한 간단한 설명을 입력해주세요'
              multiline
              maxLength={200}
            />
          </View>

          <View style={styles.section}>
            <Text style={styles.label}>참여 조건</Text>
            <View style={styles.conditionRow}>
              <View style={styles.ageContainer}>
                <Text style={styles.subLabel}>나이</Text>
                <View style={styles.ageInputContainer}>
                  <TextInput
                    style={styles.ageInput}
                    value={formData.minAge === 0 ? '' : formData.minAge.toString()}
                    onChangeText={(text) => {
                      // 빈 문자열이면 0으로 임시 저장 (화면에는 빈 문자열 표시)
                      if (text === '') {
                        setFormData((prev) => ({ ...prev, minAge: 0 }));
                        return;
                      }
                      // 숫자만 허용
                      const numValue = parseInt(text);
                      if (!isNaN(numValue) && numValue >= 0) {
                        setFormData((prev) => ({ ...prev, minAge: numValue }));
                      }
                    }}
                    onBlur={() => {
                      // 포커스를 잃을 때만 범위 체크
                      if (formData.minAge === 0) {
                        setFormData((prev) => ({ ...prev, minAge: 20 }));
                      } else {
                        const clampedValue = Math.max(20, Math.min(100, formData.minAge));
                        setFormData((prev) => ({ ...prev, minAge: clampedValue }));
                      }
                    }}
                    keyboardType='numeric'
                    maxLength={3}
                  />
                  <Text style={styles.ageText}>-</Text>
                  <TextInput
                    style={styles.ageInput}
                    value={formData.maxAge === 0 ? '' : formData.maxAge.toString()}
                    onChangeText={(text) => {
                      // 빈 문자열이면 0으로 임시 저장 (화면에는 빈 문자열 표시)
                      if (text === '') {
                        setFormData((prev) => ({ ...prev, maxAge: 0 }));
                        return;
                      }
                      // 숫자만 허용
                      const numValue = parseInt(text);
                      if (!isNaN(numValue) && numValue >= 0) {
                        setFormData((prev) => ({ ...prev, maxAge: numValue }));
                      }
                    }}
                    onBlur={() => {
                      // 포커스를 잃을 때만 범위 체크
                      if (formData.maxAge === 0) {
                        setFormData((prev) => ({ ...prev, maxAge: 20 }));
                      } else {
                        const clampedValue = Math.max(20, Math.min(100, formData.maxAge));
                        setFormData((prev) => ({ ...prev, maxAge: clampedValue }));
                      }
                    }}
                    keyboardType='numeric'
                    maxLength={3}
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
                      formData.genderCondition === option.value && styles.selectedGenderButton,
                    ]}
                    onPress={() => updateFormData('genderCondition', option.value)}
                  >
                    <Text
                      style={[
                        styles.genderButtonText,
                        formData.genderCondition === option.value && styles.selectedGenderButtonText,
                      ]}
                    >
                      {option.label}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>

      {/* 경기 선택 모달 */}
      <Modal
        visible={showGameModal}
        animationType='slide'
        presentationStyle='pageSheet'
        onRequestClose={() => setShowGameModal(false)}
      >
        <View style={[styles.modalContainer, { paddingTop: insets.top }]}>
          <View style={[styles.modalHeader, { backgroundColor: themeColor }]}>
            <TouchableOpacity onPress={() => setShowGameModal(false)}>
              <Text style={[styles.modalCancelButton, { color: '#ffffff' }]}>취소</Text>
            </TouchableOpacity>
            <Text style={[styles.modalTitle, { color: '#ffffff' }]}>경기 선택</Text>
            <View style={styles.placeholder} />
          </View>

          <FlatList
            data={games}
            keyExtractor={(item) => item.gameId.toString()}
            renderItem={({ item: game }) => (
              <TouchableOpacity
                style={[styles.gameItem, selectedGame?.gameId === game.gameId && styles.selectedGameItem]}
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
                      minute: '2-digit',
                    })}{' '}
                    | {game.stadium}
                  </Text>
                </View>
              </TouchableOpacity>
            )}
            showsVerticalScrollIndicator={false}
          />
        </View>
      </Modal>
    </View>
  );
};
