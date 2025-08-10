// entities/comment/ui/commentList.tsx
import React from 'react';
import { View, Text, Pressable, ActivityIndicator, FlatList } from 'react-native';
import { useUserStore } from '../../user';
import { useCommentStore } from '../model/store';
import { useCommentListQuery, useDeleteComment } from '../queries/useCommentQueries';
import { CommentEditForm } from './commentEditForm';
import { Comment } from '../model/types';

interface CommentListProps { postId: number }

export const CommentList: React.FC<CommentListProps> = ({ postId }) => {
  const { data, isLoading, isError } = useCommentListQuery(postId, 10);
  const deleteComment = useDeleteComment(postId);
  const { editingCommentId, setEditingCommentId } = useCommentStore();
  const myUserId = useUserStore((s) => s.currentUser?.userId); // number

  if (isLoading) return <ActivityIndicator size="large" />;
  if (isError || !data) return <Text>댓글을 불러오는 데 실패했습니다.</Text>;

  const comments: Comment[] = (data.pages ?? []).flatMap(p => p.comments ?? []);

  const renderItem = ({ item }: { item: Comment }) => {
  const isMine =
    item.isMine ?? (myUserId != null && Number((item as any).authorId ?? (item as any).userId) === myUserId);


    return (
      <View style={{ marginBottom: 16 }}>
        <Text style={{ fontWeight: 'bold' }}>{item.authorNickname}</Text>
        <Text style={{ color: 'gray', fontSize: 12 }}>
          {new Date(item.createdAt).toLocaleString()}
        </Text>

        {editingCommentId === String(item.id) ? (
          <CommentEditForm
            postId={postId}
            commentId={String(item.id)}               // ✅ number 그대로 전달
            initialContent={item.content}
          />
        ) : (
          <View style={{ marginTop: 4 }}>
            <Text>{item.isDeleted ? '(삭제된 댓글입니다)' : item.content}</Text>

            {!item.isDeleted && isMine && (
            <View style={{ flexDirection: 'row', marginTop: 4 }}>
              <Pressable onPress={() => setEditingCommentId(String(item.id))} style={{ marginRight: 12 }}>
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
  };

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
