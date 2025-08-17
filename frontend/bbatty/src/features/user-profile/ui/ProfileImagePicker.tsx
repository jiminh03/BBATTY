import React, { useState } from 'react';
import { View, Image, TouchableOpacity, ActivityIndicator, Alert, Modal, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { styles } from './ProfileImagePicker.style';

import * as ImagePicker from 'expo-image-picker';
import { uploadImageToS3 } from '../../../shared/utils/imageUpload';

interface ProfileImagePickerProps {
  imageUri?: string | null;
  onImageSelect: (url: string) => void;
  onImageRemove?: () => void;
  onUploadStart?: () => void;
  onUploadComplete?: () => void;
  isSignup?: boolean;
}

export const ProfileImagePicker: React.FC<ProfileImagePickerProps> = ({
  imageUri,
  onImageSelect,
  onImageRemove,
  onUploadStart,
  onUploadComplete,
  isSignup = false,
}) => {
  const [isUploading, setIsUploading] = useState(false);
  const [showActionModal, setShowActionModal] = useState(false);

  const pickImage = async () => {
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

      const endpoint = isSignup 
        ? '/api/auth/profile/presigned-url' 
        : '/api/posts/images/presigned-url';
      
      const uploadResult = await uploadImageToS3(asset.uri, fileName, 'profile', endpoint);

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

  const handleRemoveImage = () => {
    Alert.alert(
      '프로필 사진 삭제',
      '프로필 사진을 삭제하시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: () => {
            onImageRemove?.();
            setShowActionModal(false);
          },
        },
      ]
    );
  };

  const handlePress = () => {
    if (isUploading) return;
    
    if (imageUri && onImageRemove) {
      setShowActionModal(true);
    } else {
      pickImage();
    }
  };

  return (
    <View style={styles.container}>
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

      <Text style={styles.instructionText}>
        {imageUri ? '탭해서 프로필 사진 관리' : '탭해서 프로필 사진 추가'}
      </Text>

      {/* 액션 모달 */}
      <Modal
        visible={showActionModal}
        transparent
        animationType="fade"
        onRequestClose={() => setShowActionModal(false)}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowActionModal(false)}
        >
          <View style={styles.actionModal}>
            <Text style={styles.modalTitle}>프로필 사진</Text>
            
            <TouchableOpacity
              style={styles.actionButton}
              onPress={() => {
                setShowActionModal(false);
                pickImage();
              }}
            >
              <Ionicons name="camera" size={20} color="#007AFF" />
              <Text style={[styles.actionButtonText, { color: '#007AFF' }]}>사진 변경</Text>
            </TouchableOpacity>

            {imageUri && onImageRemove && (
              <TouchableOpacity
                style={styles.actionButton}
                onPress={handleRemoveImage}
              >
                <Ionicons name="trash" size={20} color="#FF3B30" />
                <Text style={[styles.actionButtonText, { color: '#FF3B30' }]}>사진 삭제</Text>
              </TouchableOpacity>
            )}

            <TouchableOpacity
              style={[styles.actionButton, styles.cancelButton]}
              onPress={() => setShowActionModal(false)}
            >
              <Text style={styles.cancelButtonText}>취소</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
};
