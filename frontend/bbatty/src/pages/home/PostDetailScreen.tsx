// pages/post/PostDetailScreen.tsx
import React from 'react';
import { View, Text, ActivityIndicator, StyleSheet, ScrollView } from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import { usePostDetailQuery } from '../../entities/post/queries/usePostQueries';

export default function PostDetailScreen({ route }: HomeStackScreenProps<'PostDetail'>) {
  const postId = route.params.postId;
  const { data: post, isLoading, isError, error } = usePostDetailQuery(postId);

  if (isLoading) return <View style={s.center}><ActivityIndicator size="large" /></View>;
  if (isError)   return <View style={s.center}><Text>오류: {(error as Error).message}</Text></View>;
  if (!post)     return <View style={s.center}><Text>게시글을 찾을 수 없어요.</Text></View>;

  return (
    <ScrollView style={s.container} contentContainerStyle={{ padding: 16 }}>
      <Text style={s.title}>{post.title}</Text>
      <Text style={s.meta}>
        {post.authorNickname} · {new Date(post.createdAt).toLocaleString()}
      </Text>
      <Text style={s.content}>{post.content}</Text>
      <Text style={s.stats}>👀 {post.views}   ❤️ {post.likes}   💬 {post.commentCount}</Text>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  center: { flex:1, justifyContent:'center', alignItems:'center' },
  title: { fontSize: 20, fontWeight: '700', marginBottom: 8 },
  meta: { fontSize: 12, color: '#777', marginBottom: 16 },
  content: { fontSize: 16, lineHeight: 22, color: '#222', marginBottom: 24 },
  stats: { fontSize: 12, color: '#888' },
});
