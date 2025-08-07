import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },

  scrollContent: {
    paddingHorizontal: 20,
  },

  backButton: {
    marginBottom: 30,
  },

  backButtonText: {
    fontSize: 16,
    color: '#000000',
  },

  // 프로필 이미지
  profileImageContainer: {
    alignSelf: 'center',
    marginBottom: 40,
    position: 'relative',
  },

  profileImage: {
    width: 120,
    height: 120,
    borderRadius: 60,
    backgroundColor: '#F0F0F0',
  },

  profileImagePlaceholder: {
    width: 120,
    height: 120,
    borderRadius: 60,
    alignItems: 'center',
    justifyContent: 'center',
  },

  cameraIcon: {
    width: 40,
    height: 35,
    alignItems: 'center',
    justifyContent: 'center',
  },

  cameraBody: {
    width: 30,
    height: 20,
    backgroundColor: '#CCCCCC',
    borderRadius: 4,
  },

  cameraLens: {
    position: 'absolute',
    width: 12,
    height: 12,
    backgroundColor: '#999999',
    borderRadius: 6,
  },

  cameraButton: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
    borderColor: '#FFFFFF',
  },

  cameraButtonIcon: {
    width: 16,
    height: 16,
    borderRadius: 8,
    backgroundColor: '#FFFFFF',
  },

  // 입력 필드
  inputSection: {
    marginBottom: 24,
  },

  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
  },

  required: {
    color: '#FF4444',
  },

  input: {
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
    fontSize: 16,
  },

  nicknameContainer: {
    flexDirection: 'row',
    gap: 8,
  },

  nicknameInput: {
    flex: 1,
  },

  checkButton: {
    paddingHorizontal: 20,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },

  checkButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
  },

  textArea: {
    height: 100,
    paddingTop: 12,
  },

  charCount: {
    fontSize: 12,
    textAlign: 'right',
    marginTop: 4,
  },

  errorText: {
    fontSize: 12,
    marginTop: 4,
  },

  successText: {
    fontSize: 12,
    marginTop: 4,
  },

  // 하단 버튼
  bottomContainer: {
    paddingHorizontal: 20,
    paddingTop: 20,
    backgroundColor: '#FFFFFF',
    borderTopWidth: 1,
    borderTopColor: '#F0F0F0',
  },

  submitButton: {
    height: 52,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },

  submitButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },

  // 모달
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },

  modalContent: {
    width: '100%',
    maxWidth: 320,
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
  },

  modalTitle: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 12,
  },

  modalMessage: {
    fontSize: 16,
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 24,
  },

  modalButton: {
    width: '100%',
    height: 48,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },

  modalButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
});
