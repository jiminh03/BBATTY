import { StyleSheet } from 'react-native';
import { Colors } from 'react-native/Libraries/NewAppScreen';

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
    backgroundColor: '#c4c4c4ff',
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
    color: '#dc4e4eff',
    fontSize: 12,
    marginTop: 4,
  },

  successText: {
    color: '#25d231ff',
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

  submitButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
});
