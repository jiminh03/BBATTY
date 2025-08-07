import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
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
});
