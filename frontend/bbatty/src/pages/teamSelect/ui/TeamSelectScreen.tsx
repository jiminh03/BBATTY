import React, { useState } from 'react';
import { View, Text, ScrollView, Alert } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { AuthStackScreenProps } from '../../../navigation/types';
import { haptic } from '../../../shared';
import { TeamGrid } from './TeamGrid';
import { TeamConfirmModal } from './TeamConfirmModal';
import { TEAMS } from '../../../entities/team/model/teamTypes';
import { styles } from './TeamSelectScreen.styles';

export default function TeamSelectScreen() {
  const insets = useSafeAreaInsets();
  const navigation = useNavigation<AuthStackScreenProps<'TeamSelect'>['navigation']>();

  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null);
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  const handleTeamSelect = (teamId: number) => {
    haptic.light();
    setSelectedTeamId(teamId);
    setShowConfirmModal(true);
  };

  const handleConfirm = async () => {
    if (!selectedTeamId) return;

    try {
      // 선택한 팀 정보 가져오기, 수정
      const selectedTeam = TEAMS.find((team) => team.id === selectedTeamId)!;
      navigation.navigate('SignUp', {
        teamId: selectedTeam.id,
      });
    } catch (error) {
      Alert.alert('오류', '팀 선택에 실패했습니다. 다시 시도해주세요.');
    } finally {
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
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    </View>
  );
}
