// entities/comment/ui/commentList.tsx
import React, { useEffect, useCallback } from 'react';
import { View, Text, Pressable, ActivityIndicator, FlatList } from 'react-native';
import { useUserStore } from '../../user/model/userStore';
import { useCommentStore } from '../model/store';
import { useCommentListQuery, useDeleteComment } from '../queries/useCommentQueries';
import { CommentEditForm } from './commentEditForm';
import { Comment } from '../model/types';

interface CommentListProps { postId: number }

export const CommentList: React.FC<CommentListProps> = ({ postId }) => {
  // 1) 모든 hook은 최상단에서 항상 호출
  const { data, isLoading, isError } = useCommentListQuery(postId, 10);
  const deleteComment = useDeleteComment(postId);
  const { editingCommentId, setEditingCommentId } = useCommentStore();
  const myNickname = useUserStore(s => s.currentUser?.nickname);

  // 2) data가 없어도 안전하게 계산 (빈 배열로)
  const comments: Comment[] = (data?.pages ?? []).flatMap(p => p?.comments ?? []);

  // 3) 편집 중 대상이 삭제되면 편집 종료 (항상 hook 호출됨)
  useEffect(() => {
    if (!editingCommentId) return;
    const target = comments.find(c => String(c.id) === String(editingCommentId));
    if (target?.isDeleted) setEditingCommentId(null);
  }, [comments, editingCommentId, setEditingCommentId]);

  const openEdit = useCallback((item: Comment) => {
    if (item.isDeleted) return;
    setEditingCommentId(String(item.id));
  }, [setEditingCommentId]);

  // 4) 이제 UI 분기
  if (isLoading) return <ActivityIndicator size="large" />;
  if (isError || !data) return <Text>댓글을 불러오는 데 실패했습니다.</Text>;

  const renderItem = ({ item }: { item: Comment }) => {
    const isMine =
      !!myNickname &&
      (item.authorNickname === myNickname || (item as any).nickname === myNickname);

    const isEditing = String(editingCommentId) === String(item.id);
    const displayDate = item.updatedAt ?? item.createdAt;

    // 삭제된 댓글은 버튼/에디트폼 없이 텍스트만
    if (item.isDeleted) {
      return (
        <View style={{ marginBottom: 16 }}>
          <Text style={{ fontWeight: 'bold' }}>
            {item.authorNickname ?? (item as any).nickname}
          </Text>
          <Text style={{ color: 'gray', fontSize: 12 }}>
            {new Date(displayDate).toLocaleString()}
          </Text>
          <View style={{ marginTop: 4 }}>
            <Text>(삭제된 댓글입니다)</Text>
          </View>
        </View>
      );
    }

    return (
      <View style={{ marginBottom: 16 }}>
        <Text style={{ fontWeight: 'bold' }}>
          {item.authorNickname ?? (item as any).nickname}
        </Text>
        <Text style={{ color: 'gray', fontSize: 12 }}>
          {new Date(displayDate).toLocaleString()}
        </Text>

        {isEditing ? (
          <CommentEditForm
            postId={postId}
            commentId={Number(item.id)}
            initialContent={item.content}
          />
        ) : (
          <View style={{ marginTop: 4 }}>
            <Text>{item.content}</Text>

            {isMine && (
              <View style={{ flexDirection: 'row', marginTop: 4 }}>
                <Pressable onPress={() => openEdit(item)} style={{ marginRight: 12 }}>
                  <Text style={{ color: 'blue' }}>수정</Text>
                </Pressable>
                <Pressable onPress={() => deleteComment.mutate({ commentId: Number(item.id) })}>
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
