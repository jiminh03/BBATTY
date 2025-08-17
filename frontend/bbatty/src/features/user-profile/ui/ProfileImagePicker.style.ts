import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    marginBottom: 24,
  },
  
  // 프로필 이미지
  profileImageContainer: {
    alignSelf: 'center',
    marginBottom: 12,
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
    borderColor: '#bcbcbcff',
  },

  cameraButtonIcon: {
    width: 16,
    height: 16,
    borderRadius: 8,
    backgroundColor: '#000000ff',
  },

  loadingOverlay: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
    borderColor: '#bcbcbcff',
  },

  instructionText: {
    fontSize: 14,
    color: '#666666',
    textAlign: 'center',
  },

  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },

  actionModal: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 20,
    width: '100%',
    maxWidth: 300,
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 8,
  },

  modalTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333333',
    textAlign: 'center',
    marginBottom: 20,
  },

  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 16,
    paddingHorizontal: 16,
    borderRadius: 12,
    marginBottom: 8,
    backgroundColor: '#F8F9FA',
  },

  actionButtonText: {
    fontSize: 16,
    fontWeight: '500',
    marginLeft: 12,
  },

  cancelButton: {
    backgroundColor: '#F0F0F0',
    marginTop: 8,
    justifyContent: 'center',
  },

  cancelButtonText: {
    fontSize: 16,
    fontWeight: '500',
    color: '#666666',
    textAlign: 'center',
  },
});
