import React from 'react';
import { View, Text, Modal, TouchableOpacity } from 'react-native';
// import { useTheme } from '@/shared/styles';
import { styles } from './NicknameConfilctModal.style';

interface NicknameConflictModalProps {
  visible: boolean;
  onConfirm: () => void;
}

export const NicknameConflictModal: React.FC<NicknameConflictModalProps> = ({ visible, onConfirm }) => {
  return (
    <Modal visible={visible} transparent animationType='fade' onRequestClose={onConfirm}>
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={styles.modalTitle}>중복된 닉네임</Text>

          <Text style={styles.modalMessage}>다른 사용자가 같은 닉네임을 사용하고 있습니다.</Text>

          <TouchableOpacity style={styles.modalButton} onPress={onConfirm}>
            <Text style={styles.modalButtonText}>확인</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};
