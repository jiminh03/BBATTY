import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { View, Text, Pressable, ActivityIndicator, FlatList } from 'react-native';
import { useUserStore } from '../../user/model/userStore';
import { useCommentStore } from '../model/store';
import { useCommentListQuery, useDeleteComment } from '../queries/useCommentQueries';
import { CommentEditForm } from './commentEditForm';
import { Comment } from '../model/types';

interface CommentListProps { postId: number }

export const CommentList: React.FC<CommentListProps> = ({ postId }) => {
  const { data, isLoading, isError } = useCommentListQuery(postId, 10);
  const { editingCommentId, setEditingCommentId } = useCommentStore();
  const myNickname = useUserStore(s => s.currentUser?.nickname);

  // 🧠 로컬로 "삭제된 댓글 id" 기억
  const [locallyDeleted, setLocallyDeleted] = useState<Set<number>>(new Set());
  const deleteComment = useDeleteComment(postId);

  const comments: Comment[] = useMemo(
    () => (data?.pages ?? []).flatMap(p => p?.comments ?? []),
    [data]
  );

  // 삭제 눌렀을 때 즉시 안 보이게
  const handleDelete = useCallback((id: number) => {
    setLocallyDeleted(prev => new Set(prev).add(id));
    deleteComment.mutate({ commentId: id }, {
      onError: () => {
        // 실패 시 롤백
        setLocallyDeleted(prev => {
          const next = new Set(prev); next.delete(id); return next;
        });
      },
    });
  }, [deleteComment]);

  // 편집 중 대상이 삭제되면 편집 종료
  useEffect(() => {
    if (!editingCommentId) return;
    const target = comments.find(c => String(c.id) === String(editingCommentId));
    const deletedNow = target?.isDeleted || (target ? locallyDeleted.has(Number(target.id)) : false);
    if (deletedNow) setEditingCommentId(null);
  }, [comments, editingCommentId, locallyDeleted, setEditingCommentId]);

  const openEdit = useCallback((item: Comment) => {
    // 삭제된 댓글은 편집 금지
    const isDeleted = !!item.isDeleted || locallyDeleted.has(Number(item.id));
    if (isDeleted) return;
    setEditingCommentId(String(item.id));
  }, [locallyDeleted, setEditingCommentId]);

  if (isLoading) return <ActivityIndicator size="large" />;
  if (isError || !data) return <Text>댓글을 불러오는 데 실패했습니다.</Text>;

  // entities/comment/ui/commentList.tsx
const renderItem = ({ item }: { item: Comment }) => {
  const deleted =
    item.isDeleted === true ||
    Number((item as any).isDeleted) === 1 ||
    Number((item as any).is_deleted) === 1 ||
    (typeof item.content === 'string' && /삭제된\s*댓글/.test(item.content));

  const isMine =
    !!myNickname &&
    (item.authorNickname === myNickname || (item as any).nickname === myNickname);

  const isEditing = String(editingCommentId) === String(item.id);
  const displayDate = item.updatedAt ?? item.createdAt;

  if (deleted) {
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
              <Pressable onPress={() => setEditingCommentId(String(item.id))} style={{ marginRight: 12 }}>
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
