// entities/post/ui/PostForm.tsx
import React, { useEffect, useMemo, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Switch,
  ActivityIndicator,
} from 'react-native';
import { HomeStackScreenProps } from '../../../navigation/types';
import { useCreatePost, usePostDetailQuery, useUpdatePost } from '../queries/usePostQueries';
import { useUserStore } from '../../user/model/userStore';
import { isValidPost, validatePostContent } from '../../post/utils/vaildation';
import { useThemeColor } from '../../../shared/team/ThemeContext';

type Props = HomeStackScreenProps<'PostForm'>;

export const PostForm: React.FC<Props> = ({ route, navigation }) => {
  const themeColor = useThemeColor(); // 헤더/버튼 색
  const postId = route.params?.postId;
  const isEdit = postId != null;

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

  useEffect(() => {
    if (isEdit && detail) {
      setTitle(detail.title ?? '');
      setContent(detail.content ?? '');
      // setOnlySameTeamReply(detail.onlySameTeamReply ?? false)  // 서버 스키마 맞으면 사용
    }
  }, [isEdit, detail]);

  const isSubmitting = useMemo(
    () => (isEdit ? updatePost.isPending : createPost.isPending),
    [isEdit, updatePost.isPending, createPost.isPending]
  );

  const handleSubmit = async () => {
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
        await updatePost.mutateAsync({ postId: postId!, title: t, content: c, teamId });
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
        <ScrollView contentContainerStyle={styles.contentWrap} keyboardShouldPersistTaps="handled">
          {/* 제목 */}
          <Text style={styles.label}>제목</Text>
          <TextInput
            style={styles.titleInput}
            placeholder="최대 30글자까지 입력 가능"
            placeholderTextColor="#B9BDC1"
            value={title}
            onChangeText={setTitle}
            maxLength={30}
          />

          {/* 내용 */}
          <Text style={[styles.label, { marginTop: 18 }]}>내용</Text>
          <TextInput
            style={styles.bodyInput}
            placeholder="내용을 입력해주세요."
            placeholderTextColor="#B9BDC1"
            value={content}
            onChangeText={setContent}
            multiline
            textAlignVertical="top"
          />

          {/* 하단 툴바: 이미지 버튼 + 토글 */}
          <View style={styles.toolbar}>
            <TouchableOpacity
              style={styles.imageBtn}
              onPress={() => {
                // TODO: 이미지 선택/첨부 연결
              }}
            >
              {/* 간단한 아이콘 대체 (원하면 react-native-vector-icons로 변경 가능) */}
              <Text style={{ fontSize: 20 }}>🖼️</Text>
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
            disabled={isSubmitting}
            onPress={handleSubmit}
            activeOpacity={0.9}
          >
            {isSubmitting ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.submitText}>등록하기</Text>
            )}
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
};

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#fff' },

  // HEADER
  header: {
    height: 56,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
  },
  backText: { color: '#fff', fontSize: 28, fontWeight: '600', width: 24 },
  headerTitle: { flex: 1, textAlign: 'left', marginLeft: 8, color: '#fff', fontSize: 18, fontWeight: '700' },

  // CONTENT
  contentWrap: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 24,
  },
  label: { fontSize: 16, color: '#111', fontWeight: '700', marginBottom: 8 },
  titleInput: {
    height: 44,
    borderRadius: 8,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#E3E5E7',
    backgroundColor: '#F5F6F7',
    paddingHorizontal: 12,
    fontSize: 14,
    color: '#111',
  },
  bodyInput: {
    minHeight: 280,
    borderRadius: 8,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#E3E5E7',
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 12,
    paddingVertical: 12,
    fontSize: 15,
    color: '#111',
  },

  // TOOLBAR
  toolbar: {
    marginTop: 10,
    marginBottom: 8,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  imageBtn: {
    width: 36,
    height: 36,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#D7DBE0',
    backgroundColor: '#FFF',
  },
  toggleWrap: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  toggleLabel: { fontSize: 14, color: '#333' },

  errorText: { color: '#FF3B30', marginTop: 6 },

  // BOTTOM BUTTON
  bottomBar: {
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: Platform.select({ ios: 24, android: 16 }),
  },
  submitBtn: {
    height: 48,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  submitText: { color: '#fff', fontSize: 18, fontWeight: '700' },
});

export default PostForm;
