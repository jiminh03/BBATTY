import {
  launchImageLibrary,
  ImagePickerResponse,
  ImageLibraryOptions,
  Asset,
  PhotoQuality,
} from 'react-native-image-picker';
import ImageResizer from 'react-native-image-resizer';
import { Platform, Alert, Linking } from 'react-native';
import { check, request, PERMISSIONS, RESULTS } from 'react-native-permissions';
import { API_CONFIG } from '../api/client/config';

// 이미지 선택 옵션
export interface ImagePickerOptions {
  mediaType?: 'photo' | 'video' | 'mixed';
  maxWidth?: number;
  maxHeight?: number;
  quality?: PhotoQuality;
  allowsMultiple?: boolean;
  maxFiles?: number;
}

// 이미지 리사이즈 옵션
export interface ResizeOptions {
  width: number;
  height: number;
  quality?: number;
  format?: 'JPEG' | 'PNG' | 'WEBP';
  rotation?: number;
}

// 이미지 정보 타입
export interface ImageInfo {
  uri: string;
  width: number;
  height: number;
  fileSize: number;
  fileName: string;
  type: string;
  base64?: string;
}

// 갤러리 권한 요청 (iOS 14+ 대응)
export const requestGalleryPermission = async (): Promise<boolean> => {
  if (Platform.OS === 'android') {
    // 안드로이드는 갤러리 접근에 별도 권한 불필요 (API 33 미만)
    // 추후 iOS 제작 시 READ_MEDIA_IMAGES 권한 체크 추가 필요
    return true;
  }

  try {
    const permission = PERMISSIONS.IOS.PHOTO_LIBRARY;
    const result = await check(permission);

    switch (result) {
      case RESULTS.GRANTED:
        return true;
      case RESULTS.DENIED:
        const requestResult = await request(permission);
        return requestResult === RESULTS.GRANTED;
      case RESULTS.BLOCKED:
      case RESULTS.LIMITED:
        Alert.alert('갤러리 권한 필요', '사진 선택을 위해 갤러리 권한이 필요합니다. 설정에서 권한을 허용해주세요.', [
          { text: '취소', style: 'cancel' },
          { text: '설정으로 이동', onPress: () => Linking.openSettings() },
        ]);
        return false;
      default:
        return false;
    }
  } catch (error) {
    console.error('갤러리 권한 요청 실패:', error);
    return false;
  }
};

// 갤러리에서 사진 선택
export const selectPhoto = async (options?: ImagePickerOptions): Promise<ImageInfo | ImageInfo[] | null> => {
  const hasPermission = await requestGalleryPermission();
  if (!hasPermission) return null;

  const libraryOptions: ImageLibraryOptions = {
    mediaType: options?.mediaType || 'photo',
    maxWidth: options?.maxWidth,
    maxHeight: options?.maxHeight,
    quality: options?.quality,
    selectionLimit: options?.allowsMultiple ? options.maxFiles || 10 : 1,
  };

  return new Promise((resolve) => {
    launchImageLibrary(libraryOptions, (response: ImagePickerResponse) => {
      if (response.didCancel || response.errorCode) {
        resolve(null);
        return;
      }

      const assets = response.assets || [];
      if (assets.length === 0) {
        resolve(null);
        return;
      }

      if (options?.allowsMultiple) {
        resolve(assets.map(processAsset));
      } else {
        resolve(processAsset(assets[0]));
      }
    });
  });
};

// Asset을 ImageInfo로 변환
const processAsset = (asset: Asset): ImageInfo => {
  return {
    uri: asset.uri || '',
    width: asset.width || 0,
    height: asset.height || 0,
    fileSize: asset.fileSize || 0,
    fileName: asset.fileName || `image_${Date.now()}.jpg`,
    type: asset.type || 'image/jpeg',
    base64: asset.base64,
  };
};

// 이미지 리사이즈
export const resizeImage = async (uri: string, options: ResizeOptions): Promise<ImageInfo> => {
  try {
    const response = await ImageResizer.createResizedImage(
      uri,
      options.width,
      options.height,
      options.format || 'JPEG',
      options.quality || 80,
      options.rotation || 0,
      undefined,
      false,
      {
        mode: 'contain',
        onlyScaleDown: true,
      }
    );

    return {
      uri: response.uri,
      width: response.width,
      height: response.height,
      fileSize: response.size,
      fileName: response.name || `resized_${Date.now()}.jpg`,
      type: `image/${(options.format || 'JPEG').toLowerCase()}`,
    };
  } catch (error) {
    console.error('이미지 리사이즈 실패:', error);
    throw error;
  }
};

// 이미지 파일 크기 체크
export const checkFileSize = (fileSize: number): boolean => {
  return fileSize <= API_CONFIG.upload.maxFileSize;
};

// 이미지 타입 체크
export const checkFileType = (type: string): boolean => {
  return API_CONFIG.upload.allowedTypes.includes(type);
};

// 이미지 유효성 검사
export const validateImage = (
  image: ImageInfo
): {
  isValid: boolean;
  error?: string;
} => {
  if (!checkFileSize(image.fileSize)) {
    const maxSizeMB = API_CONFIG.upload.maxFileSize / (1024 * 1024);
    return {
      isValid: false,
      error: `파일 크기는 ${maxSizeMB}MB를 초과할 수 없습니다`,
    };
  }

  if (!checkFileType(image.type)) {
    return {
      isValid: false,
      error: 'JPG, PNG, WEBP 형식만 지원됩니다',
    };
  }

  return { isValid: true };
};

// 이미지 선택 다이얼로그
export const showImagePicker = async (options?: ImagePickerOptions): Promise<ImageInfo | null> => {
  return new Promise((resolve) => {
    Alert.alert(
      '사진 선택',
      '사진을 선택해주세요',
      [
        { text: '취소', style: 'cancel', onPress: () => resolve(null) },
        {
          text: '갤러리',
          onPress: async () => {
            const photo = await selectPhoto(options);
            resolve(Array.isArray(photo) ? photo[0] : photo);
          },
        },
      ],
      { cancelable: true }
    );
  });
};
