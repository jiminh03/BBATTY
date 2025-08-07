import React, { useState } from 'react';
import { View, Text, ScrollView, Alert } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { AuthStackScreenProps } from '../../../navigation/types';
import { haptic } from '../../../shared';
// import { useUserStore } from '@/entities/user';
// import { authApi } from '@/features/auth/api';
import { TeamGrid } from './TeamGrid';
import { TeamConfirmModal } from './TeamConfirmModal';
import { TEAMS } from '../types';
import { styles } from './TeamSelectScreen.styles';

type Props = AuthStackScreenProps<'TeamSelect'>;
import { AuthStackParamList } from '../../../navigation/types';

type TeamSelectRouteProp = RouteProp<AuthStackParamList, 'TeamSelect'>;

export default function TeamSelectScreen() {
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  // const setCurrentUser = useUserStore((state) => state.setCurrentUser);

  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // route params에서 사용자 정보 가져오기
  const route = useRoute<TeamSelectRouteProp>();
  const nickname = route.params?.nickname || '사용자';

  const handleTeamSelect = (teamId: number) => {
    haptic.light();
    setSelectedTeamId(teamId);
    setShowConfirmModal(true);
  };

  const handleConfirm = async () => {
    if (!selectedTeamId) return;

    setIsLoading(true);
    try {
      // 팀 선택 API 호출
      // await authApi.selectTeam(selectedTeamId);

      // 선택한 팀 정보 가져오기
      const selectedTeam = TEAMS.find((team) => team.id === selectedTeamId);

      // 성공 메시지
      Alert.alert('팀 선택 완료', `${selectedTeam?.name}을(를) 선택하셨습니다!`, [
        {
          text: '확인',
          onPress: () => {
            // 메인 화면으로 이동
            // resetToMain();
          },
        },
      ]);
    } catch (error) {
      Alert.alert('오류', '팀 선택에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    setShowConfirmModal(false);
    setSelectedTeamId(null);
  };

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        {/* 헤더 섹션 */}
        <View style={styles.header}>
          <Text style={styles.greeting}>안녕하세요</Text>
          <View style={styles.nicknameContainer}>
            <Text style={styles.nickname}>{nickname}</Text>
            <Text style={styles.nicknameSuffix}>님!</Text>
          </View>
          <Text style={styles.question}>어느 팀을 응원하시나요?</Text>
          <Text style={styles.subText}>한 번 선택한 팀은 변경할 수 없으니 신중히 선택해주세요</Text>
        </View>

        {/* 팀 그리드 */}
        <TeamGrid teams={TEAMS} onSelectTeam={handleTeamSelect} selectedTeamId={selectedTeamId} />
      </ScrollView>

      {/* 팀 확인 모달 */}
      <TeamConfirmModal
        visible={showConfirmModal}
        teamId={selectedTeamId}
        isLoading={isLoading}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    </View>
  );
}
