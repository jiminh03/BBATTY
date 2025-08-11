// entities/comment/ui/commentList.tsx
import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { View, Text, Pressable, ActivityIndicator, FlatList, TextInput } from 'react-native';
import { useUserStore } from '../../user/model/userStore';
import { useCommentStore } from '../model/store';
import { useCommentListQuery, useDeleteComment, useCreateReply } from '../queries/useCommentQueries';
import { CommentEditForm } from './commentEditForm';
import { Comment } from '../model/types';

interface CommentListProps { postId: number }

// 서버 댓글 + 화면 메타(depth/parentId/replies) 포함해서 써먹을 로컬 타입
type CommentWithMeta = Comment & {
  depth?: number;
  parentId?: number | null;
  replies?: Comment[];   // 서버가 넣어줄 수도 있어서 남겨둠
};

// 중첩(replies)을 부모→자식 순서로 평탄화
function expandNested(list: any[], depth = 0, parentId: number | null = null): CommentWithMeta[] {
  if (!Array.isArray(list)) return [];
  return list.flatMap((c) => {
    const id = Number(c.id ?? c.commentId);
    const node: CommentWithMeta = {
      ...c,
      id,
      depth: c.depth ?? depth,  // 서버가 depth 주면 그대로, 없으면 계산값
      parentId,
      // 삭제 플래그 통일
      isDeleted: Number(c?.is_deleted ?? c?.isDeleted ?? 0) === 1 || !!c?.isDeleted,
    };
    const children = expandNested(c.replies ?? [], (node.depth ?? depth) + 1, id);
    return [node, ...children];
  });
}

export const CommentList: React.FC<CommentListProps> = ({ postId }) => {
  const { data, isLoading, isError } = useCommentListQuery(postId, 10);
  const { editingCommentId, setEditingCommentId } = useCommentStore();
  const myNickname = useUserStore((s) => s.currentUser?.nickname);
  const deleteComment = useDeleteComment(postId);

  // 즉시 숨김용 로컬 세트 (낙관적 삭제)
  const [locallyDeleted, setLocallyDeleted] = useState<Set<number>>(new Set());

  // 1) 원본 부모 댓글들
  const rawParents = useMemo(
    () => (data?.pages ?? []).flatMap((p: any) => p?.comments ?? []),
    [data]
  );
  // 2) 부모+대댓글 평탄화
  const comments = useMemo(
    () => expandNested(rawParents, 0, null),
    [rawParents]
  );

  // 삭제 눌렀을 때 즉시 안 보이게 + 실패 시 롤백
  const handleDelete = useCallback((id: number) => {
    setLocallyDeleted((prev) => new Set(prev).add(id));
    deleteComment.mutate(
      { commentId: id },
      {
        onError: () =>
          setLocallyDeleted((prev) => {
            const n = new Set(prev);
            n.delete(id);
            return n;
          }),
      }
    );
  }, [deleteComment]);

  // 편집 중 대상이 삭제되면 편집 종료
  useEffect(() => {
    if (!editingCommentId) return;
    const t = comments.find((c) => String(c.id) === String(editingCommentId));
    if (t && (t.isDeleted || locallyDeleted.has(Number(t.id)))) setEditingCommentId(null);
  }, [comments, editingCommentId, locallyDeleted, setEditingCommentId]);

  const openEdit = useCallback(
    (item: CommentWithMeta) => {
      const gone = !!item.isDeleted || locallyDeleted.has(Number(item.id));
      if (!gone) setEditingCommentId(String(item.id));
    },
    [locallyDeleted, setEditingCommentId]
  );

  if (isLoading) return <ActivityIndicator size="large" />;
  if (isError || !data) return <Text>댓글을 불러오는 데 실패했습니다.</Text>;

  return (
    <View style={{ padding: 16 }}>
      <FlatList
        data={comments}
        keyExtractor={(it) => String(it.id)}
        renderItem={({ item }) => (
          <CommentRow
            item={item}
            postId={postId}
            myNickname={myNickname}
            isEditing={String(editingCommentId) === String(item.id)}
            locallyDeleted={locallyDeleted}
            onOpenEdit={openEdit}
            onDelete={handleDelete}
          />
        )}
      />
    </View>
  );
};

/* ------------ 한 줄(댓글) 컴포넌트 ------------ */

type RowProps = {
  item: CommentWithMeta;
  postId: number;
  myNickname?: string | null;
  isEditing: boolean;
  locallyDeleted: Set<number>;
  onOpenEdit: (item: CommentWithMeta) => void;
  onDelete: (id: number) => void;
};

const CommentRow: React.FC<RowProps> = ({
  item,
  postId,
  myNickname,
  isEditing,
  locallyDeleted,
  onOpenEdit,
  onDelete,
}) => {
  const [replyOpen, setReplyOpen] = useState(false);

  const isMine =
    !!myNickname &&
    (item.authorNickname === myNickname || (item as any).nickname === myNickname);

  const isDeleted = !!item.isDeleted || locallyDeleted.has(Number(item.id));
  const displayDate = item.updatedAt ?? item.createdAt;
  const indent = Number(item.depth ?? 0) * 12;

  if (isDeleted) {
    return (
      <View style={{ marginBottom: 16, paddingLeft: indent }}>
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
    <View style={{ marginBottom: 16, paddingLeft: indent }}>
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

          <View style={{ flexDirection: 'row', marginTop: 6 }}>
            {/* 누구나 답글 가능 (삭제지만 아니면) */}
            <Pressable onPress={() => setReplyOpen((v) => !v)} style={{ marginRight: 12 }}>
              <Text style={{ color: '#007AFF' }}>
                {replyOpen ? '답글 닫기' : '답글'}
              </Text>
            </Pressable>

            {isMine && (
              <>
                <Pressable onPress={() => onOpenEdit(item)} style={{ marginRight: 12 }}>
                  <Text style={{ color: 'blue' }}>수정</Text>
                </Pressable>
                <Pressable onPress={() => onDelete(Number(item.id))}>
                  <Text style={{ color: 'red' }}>삭제</Text>
                </Pressable>
              </>
            )}
          </View>

          {replyOpen && (
            <ReplyBox
              postId={postId}
              parentId={Number(item.id)}
              onDone={() => setReplyOpen(false)}
            />
          )}
        </View>
      )}
    </View>
  );
};

/* ------------ 답글 입력 박스 ------------ */

const ReplyBox: React.FC<{ postId: number; parentId: number; onDone: () => void }> = ({
  postId,
  parentId,
  onDone,
}) => {
  const [val, setVal] = useState('');
  const createReply = useCreateReply(postId, parentId);

  const submit = () => {
    const msg = val.trim();
    if (!msg) return;
    createReply.mutate(msg, {
      onSuccess: () => {
        setVal('');
        onDone();
      },
    });
  };

  return (
    <View style={{ marginTop: 8, paddingLeft: 12 }}>
      <TextInput
        value={val}
        onChangeText={setVal}
        placeholder="답글을 입력하세요"
        style={{ borderWidth: 1, borderColor: '#ccc', borderRadius: 6, padding: 8 }}
        multiline
      />
      <View style={{ flexDirection: 'row', marginTop: 6 }}>
        <Pressable onPress={submit} style={{ marginRight: 12 }}>
          <Text style={{ color: '#007AFF', fontWeight: '600' }}>
            {createReply.isPending ? '작성 중…' : '답글 등록'}
          </Text>
        </Pressable>
        <Pressable onPress={onDone}>
          <Text style={{ color: '#888' }}>취소</Text>
        </Pressable>
      </View>
    </View>
  );
};
