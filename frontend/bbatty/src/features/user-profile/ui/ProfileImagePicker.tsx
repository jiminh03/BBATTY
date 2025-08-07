import React from 'react';
import { View, Image, TouchableOpacity } from 'react-native';
import { styles } from './ProfileImagePicker.style';

interface ProfileImagePickerProps {
  imageUri: string | null;
  onImageSelect: (url: string) => void;
  onPress: () => void;
}

export const ProfileImagePicker: React.FC<ProfileImagePickerProps> = ({ imageUri, onPress }) => {
  return (
    <TouchableOpacity style={styles.profileImageContainer} onPress={onPress} activeOpacity={0.8}>
      {imageUri ? (
        <Image source={{ uri: imageUri }} style={styles.profileImage} />
      ) : (
        <View style={[styles.profileImagePlaceholder, { backgroundColor: '#F0F0F0' }]}>
          <View style={styles.cameraIcon}>
            <View style={styles.cameraBody} />
            <View style={styles.cameraLens} />
          </View>
        </View>
      )}
      <View style={styles.cameraButton}>
        <View style={styles.cameraButtonIcon} />
      </View>
    </TouchableOpacity>
  );
};
