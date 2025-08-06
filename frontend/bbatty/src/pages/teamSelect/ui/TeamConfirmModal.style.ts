// pages/teamSelect/ui/TeamConfirmModal.styles.ts

import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 40,
  },
  modalContent: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
    width: '100%',
    maxWidth: 320,
    // 안드로이드 그림자
    elevation: 10,
    // iOS 그림자
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
  modalTeamLogo: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#F8F8F8',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  modalTeamImage: {
    width: 60,
    height: 60,
    resizeMode: 'contain',
  },
  modalTeamEmoji: {
    fontSize: 40,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#000000',
    marginBottom: 12,
  },
  modalMessage: {
    fontSize: 14,
    color: '#666666',
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: 24,
  },
  modalHighlight: {
    color: '#FF0000',
    fontWeight: 'bold',
  },
  modalButtons: {
    flexDirection: 'row',
    width: '100%',
    gap: 12, // React Native 0.71+ 지원, 이전 버전은 marginHorizontal 사용
  },
  modalButton: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 48,
  },
  modalCancelButton: {
    backgroundColor: '#F0F0F0',
  },
  modalConfirmButton: {
    backgroundColor: '#000000',
  },
  modalButtonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333333',
  },
  modalConfirmText: {
    color: '#FFFFFF',
  },
});
