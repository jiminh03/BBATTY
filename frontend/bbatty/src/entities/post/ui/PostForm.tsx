// entities/post/ui/PostForm.tsx
import React, { useEffect, useMemo, useState, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Switch,
  ActivityIndicator,
  Image,
  Alert,
  PanResponder,
  Dimensions,
  Animated,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { styles } from './PostForm.style';
import { uploadImageToS3 } from '../../../shared/utils/imageUpload';
import { postApi } from '../api/api';
import { HomeStackScreenProps } from '../../../navigation/types';
import { useCreatePost, usePostDetailQuery, useUpdatePost } from '../queries/usePostQueries';
import { useUserStore } from '../../user/model/userStore';
import { isValidPost, validatePostContent } from '../../post/utils/vaildation';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { RichTextEditor } from '../../../shared/ui/RichTextEditor';

interface ImageItem {
  id: string;
  uri: string;
  url?: string;
  isUploading?: boolean;
}

type Props = HomeStackScreenProps<'PostForm'>;

export const PostForm: React.FC<Props> = ({ route, navigation }) => {
  const themeColor = useThemeColor(); // 헤더/버튼 색
  const postId = route.params?.postId ?? null;
  const isEdit = postId !== null && postId !== undefined;

  // 작성 팀
  const teamId = useUserStore((s) => s.currentUser?.teamId) ?? 1;

  // 상세(수정 시)
  const { data: detail } = usePostDetailQuery(isEdit ? postId! : -1);

  // mutations
  const createPost = useCreatePost();
  const updatePost = useUpdatePost();

  // form state
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [onlySameTeamReply, setOnlySameTeamReply] = useState(false); // 토글(서버 연동 필요 시 payload에 포함)
  const [error, setError] = useState('');
  const [imageList, setImageList] = useState<ImageItem[]>([]);
  const [cursorPosition, setCursorPosition] = useState(0);

  // 이미지를 마크다운 형식으로 content에 삽입
  const insertImageToContent = (imageUrl: string, imageId: string) => {
    const markdown = `![image](${imageUrl})`;
    const beforeCursor = content.substring(0, cursorPosition);
    const afterCursor = content.substring(cursorPosition);
    const newContent = beforeCursor + markdown + afterCursor;
    
    console.log('[PostForm] insertImageToContent');
    console.log('[PostForm] current content:', content);
    console.log('[PostForm] cursorPosition:', cursorPosition);
    console.log('[PostForm] imageUrl:', imageUrl);
    console.log('[PostForm] markdown:', markdown);
    console.log('[PostForm] newContent:', newContent);
    
    setContent(newContent);
    setCursorPosition(cursorPosition + markdown.length);
  };

  // content에서 특정 이미지 마크다운 제거
  const removeImageFromContent = (imageUrl: string) => {
    const markdown = `![image](${imageUrl})`;
    const newContent = content.replace(markdown, '');
    setContent(newContent);
  };

  // content에서 이미지 URL들 추출
  const extractImageUrlsFromContent = (text: string): string[] => {
    const regex = /!\[image\]\(([^)]+)\)/g;
    const urls: string[] = [];
    let match;
    while ((match = regex.exec(text)) !== null) {
      urls.push(match[1]);
    }
    return urls;
  };

  useEffect(() => {
    if (isEdit && detail) {
      setTitle(detail.title ?? '');
      const detailContent = detail.content ?? '';
      setContent(detailContent);
      // setOnlySameTeamReply(detail.onlySameTeamReply ?? false)  // 서버 스키마 맞으면 사용

      // content에서 이미지 URL들 추출하여 imageList 생성
      const imageUrls = extractImageUrlsFromContent(detailContent);
      if (imageUrls.length > 0) {
        const existingImages: ImageItem[] = imageUrls.map((url, index) => ({
          id: `existing_${index}`,
          uri: url,
          url: url,
        }));
        setImageList(existingImages);
      }
    }
  }, [isEdit, detail]);

  const isSubmitting = useMemo(
    () => (isEdit ? updatePost.isPending : createPost.isPending),
    [isEdit, updatePost.isPending, createPost.isPending]
  );

  const hasUploadingImages = useMemo(() => imageList.some((img) => img.isUploading), [imageList]);

  const handleImagePick = async () => {
    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: 'images',
        allowsEditing: false,
        quality: 0.8,
        allowsMultipleSelection: false,
      });

      if (result.canceled) return;

      const asset = result.assets[0];
      const fileName = asset.fileName || `image.${asset.type?.split('/')[1] || 'jpg'}`;
      const imageId = `${Date.now()}_${Math.random().toString(36).substring(2)}`;

      // 임시 이미지 추가
      const newImage: ImageItem = {
        id: imageId,
        uri: asset.uri,
        isUploading: true,
      };

      setImageList((prev) => [...prev, newImage]);

      // S3에 업로드
      const uploadResult = await uploadImageToS3(asset.uri, fileName, 'post');

      if (uploadResult.success) {
        const imageUrl = uploadResult.data.fileUrl;
        setImageList((prev) =>
          prev.map((img) => (img.id === imageId ? { ...img, url: imageUrl, isUploading: false } : img))
        );
        // content에 이미지 마크다운 삽입
        insertImageToContent(imageUrl, imageId);
      } else {
        Alert.alert('업로드 실패', uploadResult.error.message, [{ text: '확인' }]);
        setImageList((prev) => prev.filter((img) => img.id !== imageId));
      }
    } catch (error) {
      Alert.alert('오류', '이미지 선택 중 오류가 발생했습니다.', [{ text: '확인' }]);
    }
  };

  const handleImageDelete = async (imageId: string) => {
    const image = imageList.find((img) => img.id === imageId);
    if (!image) return;

    // content에서 이미지 마크다운 제거
    if (image.url) {
      removeImageFromContent(image.url);
    }

    // 서버에 업로드된 이미지인 경우 삭제 API 호출
    if (image.url && postId) {
      try {
        await postApi.deletePostImage(postId, image.url);
      } catch (error) {
        console.warn('이미지 삭제 API 오류:', error);
      }
    }

    setImageList((prev) => prev.filter((img) => img.id !== imageId));
  };

  // 드래그 앤 드롭 상태
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);
  const dragOffsetX = useRef(new Animated.Value(0)).current;
  const dragOffsetY = useRef(new Animated.Value(0)).current;
  const dragScale = useRef(new Animated.Value(1)).current;

  const createPanResponder = (index: number) => {
    return PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      
      onPanResponderGrant: () => {
        setDraggedIndex(index);
        Animated.parallel([
          Animated.timing(dragScale, {
            toValue: 1.1,
            duration: 150,
            useNativeDriver: false,
          }),
        ]).start();
      },
      
      onPanResponderMove: (_, gestureState) => {
        dragOffsetX.setValue(gestureState.dx);
        dragOffsetY.setValue(gestureState.dy);
      },
      
      onPanResponderRelease: (_, gestureState) => {
        const dragDistance = Math.abs(gestureState.dx);
        const imageWidth = 88; // 80 + 8 margin
        const targetIndex = Math.round(dragDistance / imageWidth);
        
        if (targetIndex > 0 && targetIndex < imageList.length) {
          const newIndex = Math.min(index + Math.sign(gestureState.dx) * targetIndex, imageList.length - 1);
          if (newIndex !== index && newIndex >= 0) {
            handleImageMove(index, newIndex);
          }
        }
        
        // 애니메이션 리셋
        Animated.parallel([
          Animated.timing(dragOffsetX, {
            toValue: 0,
            duration: 200,
            useNativeDriver: false,
          }),
          Animated.timing(dragOffsetY, {
            toValue: 0,
            duration: 200,
            useNativeDriver: false,
          }),
          Animated.timing(dragScale, {
            toValue: 1,
            duration: 200,
            useNativeDriver: false,
          }),
        ]).start(() => {
          setDraggedIndex(null);
        });
      },
    });
  };

  const handleImageMove = (fromIndex: number, toIndex: number) => {
    setImageList((prev) => {
      const newList = [...prev];
      const [draggedItem] = newList.splice(fromIndex, 1);
      newList.splice(toIndex, 0, draggedItem);
      updateImageOrderInContent(newList);
      return newList;
    });
  };

  const updateImageOrderInContent = (orderedImages: ImageItem[]) => {
    // 현재 content에서 모든 이미지 마크다운 제거
    let newContent = content;
    orderedImages.forEach((img) => {
      if (img.url) {
        const markdown = `![image](${img.url})`;
        newContent = newContent.replace(markdown, '');
      }
    });

    // 새로운 순서로 이미지들을 content 끝에 추가
    orderedImages.forEach((img) => {
      if (img.url) {
        const markdown = `![image](${img.url})`;
        newContent += `\n${markdown}`;
      }
    });

    setContent(newContent.trim());
  };

  const handleSubmit = async () => {
    if (hasUploadingImages) {
      Alert.alert('업로드 중', '이미지 업로드가 완료될 때까지 기다려주세요.', [{ text: '확인' }]);
      return;
    }

    const t = title.trim();
    const c = content.trim();

    const msg = validatePostContent(t, c);
    if (!isValidPost({ title: t, content: c })) {
      setError(msg ?? '입력값을 확인해주세요.');
      return;
    }
    setError('');

    try {
      if (isEdit) {
        await updatePost.mutateAsync({
          postId: postId!,
          title: t,
          content: c,
          teamId,
        });
      } else {
        await createPost.mutateAsync({
          title: t,
          content: c,
          teamId,
          // onlySameTeamReply, // 필요 시 서버 필드명에 맞춰 포함
        });
      }
      navigation.goBack();
    } catch (e: any) {
      const errMsg = e?.response?.data?.message ?? e?.message ?? '저장 중 오류가 발생했습니다.';
      setError(errMsg);
    }
  };

  return (
    <View style={styles.root}>
      {/* 헤더 */}
      <View style={[styles.header, { backgroundColor: themeColor }]}>
        <TouchableOpacity hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }} onPress={() => navigation.goBack()}>
          <Text style={styles.backText}>{'‹'}</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{isEdit ? '게시글 수정' : '게시글 작성'}</Text>
        <View style={{ width: 24 }} />
      </View>

      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.select({ ios: 'padding', android: undefined })}
        keyboardVerticalOffset={Platform.select({ ios: 12, android: 0 })}
      >
        <ScrollView contentContainerStyle={styles.contentWrap} keyboardShouldPersistTaps='handled'>
          {/* 제목 */}
          <Text style={styles.label}>제목</Text>
          <TextInput
            style={styles.titleInput}
            placeholder='최대 30글자까지 입력 가능'
            placeholderTextColor='#B9BDC1'
            value={title}
            onChangeText={setTitle}
            maxLength={30}
          />

          {/* 내용 */}
          <Text style={[styles.label, { marginTop: 18 }]}>내용</Text>
          <RichTextEditor
            style={styles.bodyInput}
            placeholder='내용을 입력해주세요.'
            value={content}
            onChangeText={setContent}
            onSelectionChange={setCursorPosition}
            onImageDelete={(imageUrl) => {
              // imageUrl로 imageList에서 해당 이미지를 찾아서 삭제
              const targetImage = imageList.find(img => img.url === imageUrl);
              if (targetImage) {
                handleImageDelete(targetImage.id);
              }
            }}
            onImageMove={(fromImageUrl, toPosition) => {
              // 이미지 순서 변경 시 content에서 마크다운 순서도 변경
              const imageMarkdowns = extractImageUrlsFromContent(content).map(url => `![image](${url})`);
              const fromIndex = imageMarkdowns.findIndex(md => md.includes(fromImageUrl));
              
              if (fromIndex >= 0 && toPosition >= 0 && toPosition < imageMarkdowns.length) {
                // 텍스트에서 모든 이미지 마크다운 제거
                let newContent = content;
                imageMarkdowns.forEach(md => {
                  newContent = newContent.replace(md, '');
                });
                
                // 이미지 순서 변경
                const reorderedImages = [...imageMarkdowns];
                const [movedImage] = reorderedImages.splice(fromIndex, 1);
                reorderedImages.splice(toPosition, 0, movedImage);
                
                // 새로운 순서로 이미지들을 content에 추가
                reorderedImages.forEach(md => {
                  newContent += `\n${md}`;
                });
                
                setContent(newContent.trim());
                
                // imageList도 순서 변경
                const fromImageIndex = imageList.findIndex(img => img.url === fromImageUrl);
                if (fromImageIndex >= 0) {
                  setImageList(prev => {
                    const newList = [...prev];
                    const [movedItem] = newList.splice(fromImageIndex, 1);
                    newList.splice(toPosition, 0, movedItem);
                    return newList;
                  });
                }
              }
            }}
          />


          {/* 하단 툴바: 이미지 버튼 + 토글 */}
          <View style={styles.toolbar}>
            <TouchableOpacity style={styles.imageBtn} onPress={handleImagePick} disabled={hasUploadingImages}>
              {hasUploadingImages ? (
                <ActivityIndicator size='small' color='#007AFF' />
              ) : (
                <Text style={{ fontSize: 20 }}>🖼️</Text>
              )}
            </TouchableOpacity>

            {/* <View style={styles.toggleWrap}>
              <Switch
                value={onlySameTeamReply}
                onValueChange={setOnlySameTeamReply}
                trackColor={{ false: '#E5E7EB', true: '#E5E7EB' }}
                thumbColor={onlySameTeamReply ? themeColor : '#fff'}
              />
              <Text style={styles.toggleLabel}>같은 팀만 댓글 허용</Text>
            </View> */}
          </View>

          {!!error && <Text style={styles.errorText}>{error}</Text>}
        </ScrollView>

        {/* 하단 고정 버튼 */}
        <View style={[styles.bottomBar, { backgroundColor: themeColor }]}>
          <TouchableOpacity
            style={styles.submitBtn}
            disabled={isSubmitting || hasUploadingImages}
            onPress={handleSubmit}
            activeOpacity={0.9}
          >
            {isSubmitting ? <ActivityIndicator color='#fff' /> : <Text style={styles.submitText}>등록하기</Text>}
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
};

export default PostForm;
