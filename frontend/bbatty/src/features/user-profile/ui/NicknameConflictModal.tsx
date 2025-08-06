// import React from 'react';
// import { View, Text, Modal, TouchableOpacity } from 'react-native';
// import { useTheme } from '@/shared/styles';
// import { styles } from '../styles';

// interface NicknameConflictModalProps {
//   visible: boolean;
//   onConfirm: () => void;
// }

// export const NicknameConflictModal: React.FC<NicknameConflictModalProps> = ({ visible, onConfirm }) => {
//   const { theme } = useTheme();

//   return (
//     <Modal visible={visible} transparent animationType='fade' onRequestClose={onConfirm}>
//       <View style={styles.modalOverlay}>
//         <View style={[styles.modalContent, { backgroundColor: theme.colors.surface }]}>
//           <Text style={[styles.modalTitle, { color: theme.colors.text.primary }]}>중복된 닉네임</Text>

//           <Text style={[styles.modalMessage, { color: theme.colors.text.secondary }]}>
//             다른 사용자가 같은 닉네임을 사용하고 있습니다.
//           </Text>

//           <TouchableOpacity
//             style={[styles.modalButton, { backgroundColor: theme.colors.text.primary }]}
//             onPress={onConfirm}
//           >
//             <Text style={styles.modalButtonText}>확인</Text>
//           </TouchableOpacity>
//         </View>
//       </View>
//     </Modal>
//   );
// };
