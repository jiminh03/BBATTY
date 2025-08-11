// pages/post/PostDetailScreen.tsx
import React, { useLayoutEffect, useMemo, useCallback, useRef, useState, useEffect } from 'react';
import {
  View,
  Text,
  ActivityIndicator,
  StyleSheet,
  Pressable,
  Alert,
  KeyboardAvoidingView,
  Platform,
  FlatList,
} from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { HomeStackScreenProps } from '../../navigation/types';
import { usePostDetailQuery, useDeletePostMutation } from '../../entities/post/queries/usePostQueries';
import { CommentForm } from '../../entities/comment/ui/CommentForm';
import { useUserStore } from '../../entities/user/model/userStore';
import { useCommentListQuery, useDeleteComment } from '../../entities/comment/queries/useCommentQueries';
import { useCommentStore } from '../../entities/comment/model/store';
import { CommentEditForm } from '../../entities/comment/ui/commentEditForm';

type Props = HomeStackScreenProps<'PostDetail'>;
const FORM_HEIGHT = 84;

/** 화면 전용 타입 */
type UComment = {
  id: number;
  content?: string;
  createdAt?: string;
  updatedAt?: string;
  authorNickname?: string;
  nickname?: string;
  userId?: number;
  depth?: number;
  parentId?: number | null;
  isDeleted?: boolean;
  deletedAt?: string;
  deletedByUserId?: number;
  deletedByNickname?: string;
  deleteReason?: string;
  replies?: any[];
};

/** 중첩 평탄화 */
function expandNested(list: any[], depth = 0, parentId: number | null = null): UComment[] {
  if (!Array.isArray(list)) return [];
  return list.flatMap((c) => {
    const id = Number(c.id ?? c.commentId);
    const node: UComment = {
      ...(c as object),
      id,
      depth: c.depth ?? depth,
      parentId,
      isDeleted: Number((c as any)?.is_deleted ?? (c as any)?.isDeleted ?? 0) === 1 || !!(c as any)?.isDeleted,
      nickname: (c as any).nickname,
      deletedAt: (c as any).deletedAt ?? (c as any).deleted_at,
      deletedByUserId: (c as any).deletedByUserId ?? (c as any).deleted_by_user_id,
      deletedByNickname: (c as any).deletedByNickname ?? (c as any).deleted_by_nickname,
      deleteReason: (c as any).deleteReason ?? (c as any).delete_reason,
    };
    const children = expandNested((c as any).replies ?? [], (node.depth ?? depth) + 1, id);
    return [node, ...children];
  });
}

export default function PostDetailScreen({ route, navigation }: Props) {
  const postId = route.params.postId;

  /** ✅ 훅은 전부 최상단에서 호출 */
  const insets = useSafeAreaInsets();
  const { data: post, isLoading, isError, error } = usePostDetailQuery(postId);
  const myNickname = useUserStore((s) => s.currentUser?.nickname);
  const delPost = useDeletePostMutation();

  const {
    data: cmtPages,
    isLoading: cLoading,
    isError: cError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useCommentListQuery(postId, 10);

  const delComment = useDeleteComment(postId);
  const { editingCommentId, setEditingCommentId, setReplyTarget } = useCommentStore();
  const listRef = useRef<FlatList<UComment>>(null);

  /** 삭제 즉시 숨김을 위한 로컬 세트 */
  const [locallyDeleted, setLocallyDeleted] = useState<Set<number>>(new Set());

  /** 댓글 가공 */
  const rawParents = useMemo(
    () => (cmtPages?.pages ?? []).flatMap((p: any) => p?.comments ?? []),
    [cmtPages]
  );
  const flatComments = useMemo(() => expandNested(rawParents, 0, null), [rawParents]);

  /** 무한 스크롤 */
  const onEndReached = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) fetchNextPage();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  /** 게시글 헤더 액션 */
  const isMinePost = !!post && !!myNickname && post.authorNickname === myNickname;
  useLayoutEffect(() => {
    navigation.setOptions({
      headerRight: isMinePost
        ? () => (
            <View style={{ flexDirection: 'row' }}>
              <Pressable onPress={() => navigation.navigate('PostForm', { postId })} style={{ marginRight: 16 }}>
                <Text style={{ color: '#007AFF', fontWeight: '600' }}>수정</Text>
              </Pressable>
              <Pressable
                onPress={() =>
                  Alert.alert('삭제', '정말 삭제할까요?', [
                    { text: '취소', style: 'cancel' },
                    {
                      text: '삭제',
                      style: 'destructive',
                      onPress: () =>
                        delPost.mutate(postId, {
                          onSuccess: () => navigation.goBack(),
                        }),
                    },
                  ])
                }
              >
                <Text style={{ color: '#FF3B30', fontWeight: '600' }}>삭제</Text>
              </Pressable>
            </View>
          )
        : undefined,
    });
  }, [navigation, isMinePost, delPost, postId]);

  /** ✅ 이 useEffect가 return 아래에 있었던 게 에러 원인 */
  useEffect(() => {
    if (!editingCommentId) return;
    const t = flatComments.find((c) => String(c.id) === String(editingCommentId));
    if (!t) return;
    const gone = !!t.isDeleted || locallyDeleted.has(Number(t.id));
    if (gone) setEditingCommentId(null);
  }, [editingCommentId, flatComments, locallyDeleted, setEditingCommentId]);

  /** 이후에야 early return */
  if (isLoading) {
    return (
      <View style={s.center}>
        <ActivityIndicator size="large" />
      </View>
    );
  }
  if (isError || !post) {
    return (
      <View style={s.center}>
        <Text>{isError ? `오류: ${(error as Error).message}` : '게시글을 찾을 수 없어요.'}</Text>
      </View>
    );
  }

  /** 게시글 헤더 */
  const PostHeader = (
    <View style={s.headerWrap}>
      <Text style={s.title}>{post.title}</Text>
      <Text style={s.meta}>
        {post.authorNickname} · {new Date(post.createdAt).toLocaleString()}
      </Text>
      <Text style={s.content}>{post.content}</Text>
      <Text style={s.stats}>
        👀 {post.views}  ❤️ {post.likes}  💬 {post.commentCount}
      </Text>
      <View style={{ height: 16 }} />
      <Text style={s.section}>댓글</Text>
    </View>
  );

  /** 삭제 핸들러(낙관적 숨김 + 실패 롤백) */
  const handleDeleteComment = (id: number) => {
    setLocallyDeleted((prev) => {
      const n = new Set(prev);
      n.add(id);
      return n;
    });
    delComment.mutate(
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
  };

  /** 행 렌더 */
  const renderComment = ({ item }: { item: UComment }) => {
    const indent = Number(item.depth ?? 0) * 12;
    const isTopLevel = (item.depth ?? 0) === 0; // 부모만
    const displayDate = item.updatedAt ?? item.createdAt;
    const nickname = item.authorNickname ?? item.nickname;
    const isMineCmt =
      !!myNickname && (item.authorNickname === myNickname || item.nickname === myNickname);

    const gone = !!item.isDeleted || locallyDeleted.has(Number(item.id)); // 삭제 판단
    const isEditing = !gone && String(editingCommentId ?? '') === String(item.id);

  if (gone) {
    const displayDate = item.createdAt; // ← 삭제된 항목은 작성 시각 고정

    return (
      <View style={{ paddingHorizontal: 16, paddingVertical: 12, paddingLeft: 16 + indent }}>
        <Text style={{ fontWeight: 'bold' }}>
          {item.authorNickname ?? item.nickname ?? '알 수 없음'}
        </Text>
        <Text style={{ color: 'gray', fontSize: 12 }}>
          {displayDate ? new Date(displayDate).toLocaleString() : ''}
        </Text>
        <View style={{ marginTop: 6 }}>
          <Text style={{ color: '#999' }}>(삭제된 댓글입니다)</Text>
        </View>
      </View>
    );
  }



    const onReply = () => {
      setReplyTarget?.({ id: Number(item.id), nickname });
      requestAnimationFrame(() => listRef.current?.scrollToEnd({ animated: true }));
    };
    const onEdit = () => setEditingCommentId(String(item.id));

    return (
      <View style={{ paddingHorizontal: 16, paddingVertical: 12, paddingLeft: 16 + indent }}>
        <Text style={{ fontWeight: 'bold' }}>{nickname}</Text>
        <Text style={{ color: 'gray', fontSize: 12 }}>
          {displayDate ? new Date(displayDate).toLocaleString() : ''}
        </Text>

        {isEditing ? (
          <CommentEditForm
            postId={postId}
            commentId={Number(item.id)}
            initialContent={item.content ?? ''}
          />
        ) : (
          <>
            <View style={{ marginTop: 6 }}>
              <Text>{item.content}</Text>
            </View>

            <View style={{ flexDirection: 'row', marginTop: 8 }}>
              {/* 부모 댓글에만 답글 버튼 */}
              {isTopLevel && (
                <Pressable onPress={onReply} style={{ marginRight: 12 }}>
                  <Text style={{ color: '#007AFF' }}>답글</Text>
                </Pressable>
              )}
              {/* 본인 댓글만 수정/삭제 */}
              {isMineCmt && (
                <>
                  <Pressable onPress={onEdit} style={{ marginRight: 12 }}>
                    <Text style={{ color: '#0A84FF' }}>수정</Text>
                  </Pressable>
                  <Pressable onPress={() => handleDeleteComment(Number(item.id))}>
                    <Text style={{ color: '#FF3B30' }}>삭제</Text>
                  </Pressable>
                </>
              )}
            </View>
          </>
        )}
      </View>
    );
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1, backgroundColor: '#fff' }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? insets.top : 0}
    >
      <SafeAreaView style={{ flex: 1 }}>
        <View style={{ flex: 1 }}>
          <FlatList<UComment>
            ref={listRef}
            data={flatComments}
            keyExtractor={(it) => String(it.id)}
            renderItem={renderComment}
            ListHeaderComponent={PostHeader}
            ListEmptyComponent={
              cLoading ? (
                <ActivityIndicator style={{ margin: 16 }} />
              ) : cError ? (
                <Text style={{ margin: 16 }}>댓글을 불러오는 데 실패했습니다.</Text>
              ) : (
                <Text style={{ margin: 16, color: '#777' }}>첫 댓글을 남겨보세요.</Text>
              )
            }
            onEndReachedThreshold={0.4}
            onEndReached={onEndReached}
            ListFooterComponent={
              isFetchingNextPage ? <ActivityIndicator style={{ marginVertical: 12 }} /> : <View />
            }
            contentContainerStyle={{ paddingBottom: FORM_HEIGHT + insets.bottom + 12 }}
          />

          {/* 푸터 고정 입력창 */}
          <CommentForm postId={postId} style={[s.footer, { paddingBottom: Math.max(12, insets.bottom) }]} />
        </View>
      </SafeAreaView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#fff' },
  headerWrap: { paddingHorizontal: 16, paddingTop: 16, backgroundColor: '#fff' },
  title: { fontSize: 20, fontWeight: '700', marginBottom: 8 },
  meta: { fontSize: 12, color: '#777', marginBottom: 16 },
  content: { fontSize: 16, lineHeight: 22, color: '#222', marginBottom: 24 },
  stats: { fontSize: 12, color: '#888' },
  section: { fontSize: 16, fontWeight: '600', marginTop: 8 },
  footer: { position: 'absolute', left: 0, right: 0, bottom: 0, borderTopWidth: 1, borderColor: '#eee', backgroundColor: '#fff' },
});
