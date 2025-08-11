// pages/post/PostDetailScreen.tsx
import React, { useLayoutEffect, useMemo } from 'react';
import { View, Text, ActivityIndicator, StyleSheet, ScrollView, Pressable, Alert } from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import { usePostDetailQuery, useDeletePostMutation } from '../../entities/post/queries/usePostQueries';
import { CommentForm } from '../../entities/comment/ui/CommentForm';
import { CommentList } from '../../entities/comment/ui/commentList';
import { useUserStore } from '../../entities/user/model/userStore';

type Props = HomeStackScreenProps<'PostDetail'>;

export default function PostDetailScreen({ route, navigation }: Props) {
  const postId = route.params.postId;
  const { data: post, isLoading, isError, error } = usePostDetailQuery(postId);
  const myUserId = useUserStore((s) => s.currentUser?.userId);

  // 실제 내려오는 키 확인용(개발 중 로그)
  useLayoutEffect(() => {
    if (post) console.log('[post detail] raw:', post);
  }, [post]);

  // 작성자 id 후보들 중 하나 선택
  const authorAny = useMemo(() => {
    const p: any = post ?? {};
    return p.authorId ?? p.authorUserId ?? p.userId ?? p.writerId ?? null;
  }, [post]);

  const myNickname = useUserStore(s => s.currentUser?.nickname);
  const isMine = !!post && !!myNickname && post.authorNickname === myNickname;
  const del = useDeletePostMutation();

  const handleEdit = () => navigation.navigate('PostForm', { postId });

  const handleDelete = () => {
    Alert.alert('삭제', '정말 삭제할까요?', [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제',
        style: 'destructive',
        onPress: () =>
          del.mutate(postId, {
            onSuccess: () => navigation.goBack(),
          }),
      },
    ]);
  };

  // 헤더 버튼
  useLayoutEffect(() => {
    navigation.setOptions({
      headerRight: isMine
        ? () => (
            <View style={{ flexDirection: 'row' }}>
              <Pressable onPress={handleEdit} style={{ marginRight: 16 }}>
                <Text style={{ color: '#007AFF', fontWeight: '600' }}>수정</Text>
              </Pressable>
              <Pressable onPress={handleDelete}>
                <Text style={{ color: '#FF3B30', fontWeight: '600' }}>삭제</Text>
              </Pressable>
            </View>
          )
        : undefined,
    });
  }, [navigation, isMine]);

  if (isLoading)
    return (
      <View style={s.center}>
        <ActivityIndicator size="large" />
      </View>
    );

  if (isError)
    return (
      <View style={s.center}>
        <Text>오류: {(error as Error).message}</Text>
      </View>
    );

  if (!post)
    return (
      <View style={s.center}>
        <Text>게시글을 찾을 수 없어요.</Text>
      </View>
    );

  return (
    <View style={s.container}>
      <ScrollView style={s.postContainer} contentContainerStyle={{ padding: 16 }}>
        <Text style={s.title}>{post.title}</Text>
        <Text style={s.meta}>
          {post.authorNickname} · {new Date(post.createdAt).toLocaleString()}
        </Text>
        <Text style={s.content}>{post.content}</Text>
        <Text style={s.stats}>
          👀 {post.views} ❤️ {post.likes} 💬 {post.commentCount}
        </Text>
      </ScrollView>

      {/* 댓글 작성 */}
      <CommentForm postId={postId} />

      {/* 댓글 목록 */}
      <CommentList postId={postId} />
    </View>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  postContainer: { flexGrow: 0 },
  title: { fontSize: 20, fontWeight: '700', marginBottom: 8 },
  meta: { fontSize: 12, color: '#777', marginBottom: 16 },
  content: { fontSize: 16, lineHeight: 22, color: '#222', marginBottom: 24 },
  stats: { fontSize: 12, color: '#888' },
});
