// entities/post/ui/PostForm.tsx
import React, { useEffect, useMemo, useState, useRef, useCallback } from 'react';
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

  // 상세(수정 시) - 수정 모드일 때만 조회
  const { data: detail } = usePostDetailQuery(isEdit ? postId! : null);

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
  
  // 드래그 오버레이 상태 - ref로 안정화
  const [dragOverlayInfo, setDragOverlayInfo] = useState<{
    imageUrl: string;
    startPosition: { x: number; y: number };
  } | null>(null);
  
  // 스크롤 뷰 참조
  const scrollViewRef = useRef<ScrollView>(null);
  
  const dragOffsetX = useRef(new Animated.Value(0)).current;
  const dragOffsetY = useRef(new Animated.Value(0)).current;
  const dragScale = useRef(new Animated.Value(1)).current;
  const dragOpacity = useRef(new Animated.Value(1)).current;
  
  // 드래그 상태를 ref로 보호
  const isDraggingRef = useRef(false);
  const currentDragInfoRef = useRef<{ imageUrl: string; startPosition: { x: number; y: number } } | null>(null);

  // 이미지를 마크다운 형식으로 content에 삽입
  const insertImageToContent = (imageUrl: string, imageId: string) => {
    const markdown = `![image](${imageUrl})`;
    const beforeCursor = content.substring(0, cursorPosition);
    const afterCursor = content.substring(cursorPosition);
    
    // 현재 포커스된 곳에서 내용이 없을 시 해당 위치에 이미지가 업로드되고 포커스가 이미지 아래로 향하도록 수정
    let newContent;
    if (beforeCursor.endsWith('\n') || beforeCursor === '') {
      // 줄의 시작이거나 빈 상태일 때는 그대로 삽입
      newContent = beforeCursor + markdown + (afterCursor.startsWith('\n') ? afterCursor : '\n' + afterCursor);
    } else {
      // 텍스트 중간일 때는 앞에 줄바꿈 추가
      newContent = beforeCursor + '\n' + markdown + (afterCursor.startsWith('\n') ? afterCursor : '\n' + afterCursor);
    }
    
    setContent(newContent);
    // 포커스를 이미지 아래로 이동
    const newCursorPosition = beforeCursor.length + (beforeCursor.endsWith('\n') || beforeCursor === '' ? 0 : 1) + markdown.length + 1;
    setCursorPosition(newCursorPosition);
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

    // 게시글 수정 중일 때만 S3 삭제 API 호출
    if (image.url && isEdit && postId) {
      try {
        await postApi.deletePostImage(postId, image.url);
      } catch (error) {
        console.warn('이미지 삭제 API 오류:', error);
        // 이미지 삭제 실패 시에도 UI에서는 제거 (UX 개선)
      }
    }

    setImageList((prev) => prev.filter((img) => img.id !== imageId));
  };

  // 드래그 핸들러들 - 스크롤 방지 기능 추가
  const handleDragStart = useCallback((dragInfo: { imageUrl: string; startPosition: { x: number; y: number } }) => {
    console.log('🟢 handleDragStart called:', {
      isDragging: isDraggingRef.current,
      startPosition: dragInfo.startPosition,
      timestamp: Date.now()
    });
    
    // 이미 드래그 중이면 무시 (중복 호출 방지)
    if (isDraggingRef.current) {
      console.log('🔴 handleDragStart IGNORED - already dragging');
      return;
    }
    
    isDraggingRef.current = true;
    currentDragInfoRef.current = dragInfo;
    setDragOverlayInfo(dragInfo);
    
    // 스크롤 비활성화
    if (scrollViewRef.current) {
      scrollViewRef.current.setNativeProps({ scrollEnabled: false });
    }
    
    // 애니메이션 초기화
    dragOffsetX.setValue(0);
    dragOffsetY.setValue(0);
    dragScale.setValue(0.8);
    dragOpacity.setValue(0.7);
    
    console.log('✅ handleDragStart completed');
  }, []); // 의존성 없음 - 완전히 격리

  const handleDragMove = useCallback((dragOffset: { dx: number; dy: number }) => {
    // console.log('🟡 handleDragMove called:', { dx: dragOffset.dx, dy: dragOffset.dy, isDragging: isDraggingRef.current });
    
    // 드래그 중이 아니면 무시
    if (!isDraggingRef.current) return;
    
    dragOffsetX.setValue(dragOffset.dx);
    dragOffsetY.setValue(dragOffset.dy);
  }, []); // 의존성 없음 - 완전히 격리

  const handleDragEnd = useCallback(() => {
    console.log('🟠 handleDragEnd called');
    
    isDraggingRef.current = false;
    currentDragInfoRef.current = null;
    setDragOverlayInfo(null);
    
    // 스크롤 다시 활성화
    if (scrollViewRef.current) {
      scrollViewRef.current.setNativeProps({ scrollEnabled: true });
    }
    
    // 애니메이션 리셋
    dragOffsetX.setValue(0);
    dragOffsetY.setValue(0);
    dragScale.setValue(1);
    dragOpacity.setValue(1);
    
    console.log('✅ handleDragEnd completed');
  }, []); // 의존성 없음 - 완전히 격리





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

      <View style={{ flex: 1 }}>
        <KeyboardAvoidingView
          style={{ flex: 1 }}
          behavior={Platform.select({ ios: 'padding', android: 'height' })}
          keyboardVerticalOffset={Platform.select({ ios: 88, android: 88 })}
        >
          <ScrollView 
            ref={scrollViewRef}
            contentContainerStyle={styles.contentWrap} 
            keyboardShouldPersistTaps='handled'
            showsVerticalScrollIndicator={false}
            keyboardDismissMode='interactive'
            automaticallyAdjustKeyboardInsets={true}
            style={{ flex: 1 }}
            scrollEnabled={!isDraggingRef.current}
          >
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
            onDragStart={handleDragStart}
            onDragMove={handleDragMove}
            onDragEnd={handleDragEnd}
          />


          {!!error && <Text style={styles.errorText}>{error}</Text>}
          </ScrollView>
          
          {/* 하단 툴바: 이미지 버튼 - 키보드 위에 고정 */}
          <View style={styles.toolbar}>
            <TouchableOpacity style={styles.imageBtn} onPress={handleImagePick} disabled={hasUploadingImages}>
              {hasUploadingImages ? (
                <ActivityIndicator size='small' color='#007AFF' />
              ) : (
                <Text style={{ fontSize: 20 }}>🖼️</Text>
              )}
            </TouchableOpacity>
          </View>
        </KeyboardAvoidingView>
      </View>

      {/* 하단 고정 버튼 - KeyboardAvoidingView 외부로 이동 */}
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
      
      {/* 최상위 드래그 오버레이 - 완전히 격리된 위치 관리 */}
      {dragOverlayInfo && (
        <Animated.View
          style={{
            position: 'absolute',
            left: dragOverlayInfo.startPosition.x,
            top: dragOverlayInfo.startPosition.y,
            width: 200,
            height: 200,
            transform: [
              { translateX: dragOffsetX },
              { translateY: dragOffsetY },
              { scale: dragScale }
            ],
            opacity: dragOpacity,
            zIndex: 99999,
            elevation: 20,
            pointerEvents: 'none',
            shadowColor: '#000',
            shadowOffset: { width: 0, height: 8 },
            shadowOpacity: 0.3,
            shadowRadius: 12,
          }}
        >
          <Image
            source={{ uri: dragOverlayInfo.imageUrl }}
            style={{
              width: '100%',
              height: '100%',
              borderRadius: 8,
              backgroundColor: '#F5F6F7',
            }}
            resizeMode="cover"
          />
        </Animated.View>
      )}
    </View>
  );
};

export default PostForm;
