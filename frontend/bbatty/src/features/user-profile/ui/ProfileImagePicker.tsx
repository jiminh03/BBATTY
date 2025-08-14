import React, { useState } from 'react';
import { View, Image, TouchableOpacity, ActivityIndicator, Alert } from 'react-native';
import { styles } from './ProfileImagePicker.style';

import * as ImagePicker from 'expo-image-picker';
import { uploadImageToS3 } from '../../../shared/utils/imageUpload';

interface ProfileImagePickerProps {
  imageUri?: string | null;
  onImageSelect: (url: string) => void;
  onUploadStart?: () => void;
  onUploadComplete?: () => void;
}

export const ProfileImagePicker: React.FC<ProfileImagePickerProps> = ({
  imageUri,
  onImageSelect,
  onUploadStart,
  onUploadComplete,
}) => {
  const [isUploading, setIsUploading] = useState(false);

  const handlePress = async () => {
    if (isUploading) return;

    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: 'images',
        allowsEditing: true,
        aspect: [1, 1],
        quality: 0.8,
        allowsMultipleSelection: false,
      });

      if (result.canceled) return;

      const asset = result.assets[0];
      const fileName = asset.fileName || `image.${asset.type?.split('/')[1] || 'jpg'}`;

      setIsUploading(true);
      onUploadStart?.();

      const uploadResult = await uploadImageToS3(asset.uri, fileName, 'profile');

      if (uploadResult.success) {
        onImageSelect(uploadResult.data.fileUrl);
      } else {
        Alert.alert('업로드 실패', uploadResult.error.message, [{ text: '확인' }]);
      }
    } catch (error) {
      Alert.alert('오류', '이미지 선택 중 오류가 발생했습니다.', [{ text: '확인' }]);
    } finally {
      setIsUploading(false);
      onUploadComplete?.();
    }
  };

  return (
    <TouchableOpacity
      style={[styles.profileImageContainer, isUploading && { opacity: 0.7 }]}
      onPress={handlePress}
      activeOpacity={0.8}
      disabled={isUploading}
    >
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

      {isUploading ? (
        <View style={styles.loadingOverlay}>
          <ActivityIndicator size='small' color='#007AFF' />
        </View>
      ) : (
        <View style={styles.cameraButton}>
          <View style={styles.cameraButtonIcon} />
        </View>
      )}
    </TouchableOpacity>
  );
};
