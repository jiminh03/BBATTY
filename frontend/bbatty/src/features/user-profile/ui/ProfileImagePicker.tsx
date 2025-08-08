import React from 'react';
import { View, Image, TouchableOpacity } from 'react-native';
import { styles } from './ProfileImagePicker.style';

import * as ImagePicker from 'expo-image-picker';

interface ProfileImagePickerProps {
  imageUri?: string | null;
  onImageSelect: (url: string) => void;
}

export const ProfileImagePicker: React.FC<ProfileImagePickerProps> = ({ imageUri, onImageSelect }) => {
  const handlePress = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: 'images',
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.8,
      allowsMultipleSelection: false,
    });

    if (!result.canceled) {
      onImageSelect(result.assets[0].uri);
    }
  };

  return (
    <TouchableOpacity style={styles.profileImageContainer} onPress={handlePress} activeOpacity={0.8}>
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
