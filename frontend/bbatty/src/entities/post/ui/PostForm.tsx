// entities/post/ui/PostForm.tsx
import React, { useEffect, useState } from 'react';
import { View, TextInput, Button, Text, StyleSheet } from 'react-native';
import { useCreatePost, usePostDetailQuery, useUpdatePost } from '../queries/usePostQueries';
import { HomeStackScreenProps } from '../../../navigation/types';
import { useUserStore } from '../../user/model/userStore';
import { isValidPost, validatePostContent } from '../../post/utils/vaildation';

type Props = HomeStackScreenProps<'PostForm'>;

export const PostForm: React.FC<Props> = ({ route, navigation }) => {
  const postId = route.params?.postId;
  const isEdit = postId != null;

  const teamId = useUserStore(s => s.currentUser?.teamId) ?? 1;

  const { data: detail } = usePostDetailQuery(isEdit ? postId! : -1);
  const createPost = useCreatePost();
  const updatePost = useUpdatePost();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (isEdit && detail) {
      setTitle(detail.title ?? '');
      setContent(detail.content ?? '');
    }
  }, [isEdit, detail]);

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
        // ✅ teamId 같이 전달
        await updatePost.mutateAsync({ postId: postId!, title: t, content: c, teamId });
      } else {
        await createPost.mutateAsync({ title: t, content: c, teamId });
      }
      navigation.goBack();
    } catch (e: any) {
      const errMsg = e?.response?.data?.message ?? e?.message ?? '저장 중 오류가 발생했습니다.';
      setError(errMsg);
    }
  };

  return (
    <View style={styles.container}>
      <TextInput style={styles.input} placeholder="제목" value={title} onChangeText={setTitle} />
      <TextInput style={[styles.input, styles.textArea]} placeholder="내용" value={content} onChangeText={setContent} multiline />
      {!!error && <Text style={styles.error}>{error}</Text>}
      <Button
        title={isEdit ? (updatePost.isPending ? '수정 중...' : '게시글 수정') : (createPost.isPending ? '생성 중...' : '게시글 생성')}
        onPress={handleSubmit}
        disabled={isEdit ? updatePost.isPending : createPost.isPending}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { padding: 16, backgroundColor: '#fff', flex: 1 },
  input: { borderWidth: 1, borderColor: '#ccc', borderRadius: 4, padding: 12, marginBottom: 12 },
  textArea: { height: 120, textAlignVertical: 'top' },
  error: { color: 'red', marginBottom: 8 },
});
