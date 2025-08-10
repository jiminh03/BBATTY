import React from 'react';
import { View, Text, Modal, TouchableOpacity, ActivityIndicator } from 'react-native';
import { TEAMS } from '../../../shared/team/teamTypes';
import { styles } from './TeamConfirmModal.style';

interface TeamConfirmModalProps {
  visible: boolean;
  teamId: number | null;
  onConfirm: () => void;
  onCancel: () => void;
}

export const TeamConfirmModal: React.FC<TeamConfirmModalProps> = ({ visible, teamId, onConfirm, onCancel }) => {
  const selectedTeam = teamId ? TEAMS.find((team) => team.id === teamId) : null;

  if (!selectedTeam) return null;

  return (
    <Modal visible={visible} transparent animationType='fade' onRequestClose={onCancel}>
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <View style={styles.modalTeamLogo}>
            <Text style={styles.modalTeamEmoji}>{selectedTeam.name}</Text>
          </View>
          <Text style={styles.modalTitle}>{selectedTeam.name}</Text>
          <Text style={styles.modalMessage}>
            한 번 선택한 팀은 <Text style={styles.modalHighlight}>변경 불가능</Text>합니다.
          </Text>

          <View style={styles.modalButtons}>
            <TouchableOpacity style={[styles.modalButton, styles.modalCancelButton]} onPress={onCancel}>
              <Text style={styles.modalButtonText}>취소</Text>
            </TouchableOpacity>

            <TouchableOpacity style={[styles.modalButton, styles.modalConfirmButton]} onPress={onConfirm}>
              <Text style={[styles.modalButtonText, styles.modalConfirmText]}>확정</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};
