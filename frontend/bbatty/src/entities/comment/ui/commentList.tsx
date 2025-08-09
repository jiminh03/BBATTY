// entities/comment/ui/commentList.tsx
import React from 'react';
import { View, Text, Pressable, ActivityIndicator, FlatList } from 'react-native';
import { Comment } from '../model/types';
import { useCommentStore } from '../model/store';
import { useCommentListQuery, useDeleteComment } from '../queries/useCommentQueries';
import { CommentEditForm } from './commentEditForm';

interface CommentListProps {
  postId: number;
}

export const CommentList: React.FC<CommentListProps> = ({ postId }) => {
  const { data, isLoading, isError } = useCommentListQuery(postId, 10);
  const deleteComment = useDeleteComment(postId);
  const { editingCommentId, setEditingCommentId } = useCommentStore();

  if (isLoading) return <ActivityIndicator size="large" />;
  if (isError || !data) return <Text>댓글을 불러오는 데 실패했습니다.</Text>;

  // ✅ 안전하게 pages 접근
  const comments: Comment[] = data?.pages?.flatMap((p) => p.comments ?? []) ?? [];

  const renderItem = ({ item }: { item: Comment }) => (
    <View style={{ marginBottom: 16 }}>
      <Text style={{ fontWeight: 'bold' }}>{item.authorNickname}</Text>
      <Text style={{ color: 'gray', fontSize: 12 }}>
        {new Date(item.createdAt).toLocaleString()}
      </Text>

      {editingCommentId === item.id ? (
        <CommentEditForm commentId={item.id} initialContent={item.content} />
      ) : (
        <View style={{ marginTop: 4 }}>
          <Text>{item.isDeleted ? '(삭제된 댓글입니다)' : item.content}</Text>
          {item.isMine && !item.isDeleted && (
            <View style={{ flexDirection: 'row', marginTop: 4 }}>
              <Pressable onPress={() => setEditingCommentId(item.id)} style={{ marginRight: 12 }}>
                <Text style={{ color: 'blue' }}>수정</Text>
              </Pressable>
              <Pressable onPress={() => deleteComment.mutate({ commentId: item.id })}>
                <Text style={{ color: 'red' }}>삭제</Text>
              </Pressable>
            </View>
          )}
        </View>
      )}
    </View>
  );

  return (
    <View style={{ padding: 16 }}>
      <FlatList
        data={comments}
        renderItem={renderItem}
        keyExtractor={(item) => String(item.id)}
      />
    </View>
  );
};
