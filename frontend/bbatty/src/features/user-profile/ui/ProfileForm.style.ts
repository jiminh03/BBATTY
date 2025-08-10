import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
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
    borderColor: '#E0E0E0',
  },

  // 닉네임 관련 스타일
  nicknameContainer: {
    flexDirection: 'row',
    gap: 8,
  },

  nicknameInput: {
    flex: 1,
  },

  // 닉네임 입력 상태별 스타일
  inputError: {
    borderColor: '#dc4e4e',
  },

  inputSuccess: {
    borderColor: '#25d231',
  },

  checkButton: {
    paddingHorizontal: 20,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#c4c4c4',
  },

  checkButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
  },

  // 닉네임 상태 메시지 컨테이너
  nicknameStatusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
  },

  nicknameStatusText: {
    fontSize: 12,
    marginLeft: 4,
    color: '#666',
  },

  // 텍스트 영역
  textArea: {
    height: 100,
    paddingTop: 12,
    textAlignVertical: 'top',
  },

  // 자기소개 하단 정보 컨테이너
  introductionFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 4,
  },

  introductionFooterEmpty: {
    // 에러가 없을 때 빈 공간 유지용
  },

  charCount: {
    fontSize: 12,
    textAlign: 'right',
    color: '#666',
  },

  // 상태별 텍스트 스타일
  errorText: {
    color: '#dc4e4e',
    fontSize: 12,
    marginTop: 4,
  },

  successText: {
    color: '#25d231',
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
    backgroundColor: '#CCCCCC',
  },

  submitButtonActive: {
    backgroundColor: '#1D467F',
  },

  submitButtonDisabled: {
    backgroundColor: '#CCCCCC',
  },

  submitButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
});
