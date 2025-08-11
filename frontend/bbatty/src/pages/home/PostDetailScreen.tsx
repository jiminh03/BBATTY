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
  replies?: any[];
};

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
    };
    const children = expandNested((c as any).replies ?? [], (node.depth ?? depth) + 1, id);
    return [node, ...children];
  });
}

export default function PostDetailScreen({ route, navigation }: Props) {
  const postId = route.params.postId;
  // 삭제 직전 updatedAt 캐시 (댓글/답글 공통)
  const [preDeleteUpdatedAt, setPreDeleteUpdatedAt] = useState<Map<number, string>>(new Map());


  // 훅: 항상 최상단
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

  // 낙관적 삭제 캐시
  const [locallyDeleted, setLocallyDeleted] = useState<Set<number>>(new Set());

  // ⋯ 메뉴 오픈된 댓글 id
  const [menuOpenId, setMenuOpenId] = useState<number | null>(null);

  const rawParents = useMemo(
    () => (cmtPages?.pages ?? []).flatMap((p: any) => p?.comments ?? []),
    [cmtPages]
  );
  const flatComments = useMemo(() => expandNested(rawParents, 0, null), [rawParents]);

  const onEndReached = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) fetchNextPage();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

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

  // 편집 중 대상이 사라지면 편집 종료
  useEffect(() => {
    if (!editingCommentId) return;
    const t = flatComments.find((c) => String(c.id) === String(editingCommentId));
    if (!t) return;
    const gone = !!t.isDeleted || locallyDeleted.has(Number(t.id));
    if (gone) setEditingCommentId(null);
  }, [editingCommentId, flatComments, locallyDeleted, setEditingCommentId]);

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

  // 삭제 핸들러(낙관적 숨김 + 실패 롤백)
  const handleDeleteComment = (id: number) => {
  // ✅ 삭제 누른 순간의 updatedAt을 먼저 저장
  const target = flatComments.find((c) => Number(c.id) === id);
  if (target?.updatedAt) {
    setPreDeleteUpdatedAt((m) => new Map(m).set(id, target.updatedAt!));
  }

  setMenuOpenId(null);
  setLocallyDeleted((prev) => {
    const n = new Set(prev);
    n.add(id);
    return n;
  });

  delComment.mutate(
    { commentId: id },
    {
      onError: () => {
        // 실패 시 롤백
        setLocallyDeleted((prev) => {
          const n = new Set(prev);
          n.delete(id);
          return n;
        });
        setPreDeleteUpdatedAt((m) => {
          const n = new Map(m);
          n.delete(id);
          return n;
        });
      },
    }
  );
};


  // 한 줄 렌더
  const renderComment = ({ item }: { item: UComment }) => {
    const indent = Number(item.depth ?? 0) * 12;
    const isTopLevel = (item.depth ?? 0) === 0;
    const displayDate = item.updatedAt ?? item.createdAt; // 삭제 시에도 updatedAt 우선
    const nickname = item.authorNickname ?? item.nickname;
    const isMineCmt =
      !!myNickname && (item.authorNickname === myNickname || item.nickname === myNickname);

    const gone = !!item.isDeleted || locallyDeleted.has(Number(item.id));
    const isEditing = !gone && String(editingCommentId ?? '') === String(item.id);
    const isMenuOpen = !gone && isMineCmt && menuOpenId === item.id;

    if (gone) {
  // ✅ 캐시에 있으면 그걸 사용 → 삭제시각 대신 원래 updatedAt
  const displayWhen =
    preDeleteUpdatedAt.get(Number(item.id))   // 내가 삭제 누른 직전의 updatedAt
    ?? item.updatedAt                         // 서버가 편집시각을 유지한다면 이 값
    ?? item.createdAt;                        // 최후 수단

  return (
    <View style={{ paddingHorizontal: 16, paddingVertical: 12, paddingLeft: 16 + indent }}>
      <Text style={{ fontWeight: 'bold' }}>{nickname ?? '알 수 없음'}</Text>
      <Text style={{ color: 'gray', fontSize: 12 }}>
        {displayWhen ? new Date(displayWhen).toLocaleString() : ''}
      </Text>
      <View style={{ marginTop: 6 }}>
        <Text style={{ color: '#999' }}>(삭제된 댓글입니다)</Text>
      </View>
    </View>
  );
}


    const onReply = () => {
      setMenuOpenId(null);
      setReplyTarget?.({ id: Number(item.id), nickname });
      requestAnimationFrame(() => listRef.current?.scrollToEnd({ animated: true }));
    };
    const onEdit = () => {
      setMenuOpenId(null);
      setEditingCommentId(String(item.id));
    };
    const onDelete = () => handleDeleteComment(Number(item.id));

    return (
      <View
        style={{ paddingHorizontal: 16, paddingVertical: 12, paddingLeft: 16 + indent, overflow: 'visible' }}
        collapsable={false}
      >
        {/* 사용자 + 날짜 */}
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <View style={{ flex: 1 }}>
            <Text style={{ fontWeight: 'bold' }}>{nickname}</Text>
            <Text style={{ color: 'gray', fontSize: 12 }}>
              {displayDate ? new Date(displayDate).toLocaleString() : ''}
            </Text>
          </View>

          {/* ⋯ 메뉴 버튼: 본인 댓글에만 */}
          {isMineCmt && (
            <Pressable
              onPress={() => setMenuOpenId((prev) => (prev === item.id ? null : item.id))}
              hitSlop={10}
              style={{ paddingHorizontal: 8, paddingVertical: 4 }}
            >
              <Text style={{ fontSize: 20, lineHeight: 20 }}>⋯</Text>
            </Pressable>
          )}
        </View>

        {isEditing ? (
          <CommentEditForm postId={postId} commentId={Number(item.id)} initialContent={item.content ?? ''} />
        ) : (
          <>
            <View style={{ marginTop: 6 }}>
              <Text>{item.content}</Text>
            </View>

            {/* 액션 영역: 답글 버튼 + ⋯ 메뉴 팝오버 */}
            <View style={{ marginTop: 8 }}>
              {/* 부모만 답글 */}
              {isTopLevel && (
                <Pressable onPress={onReply} style={{ marginRight: 12, alignSelf: 'flex-start' }}>
                  <Text style={{ color: '#007AFF' }}>답글</Text>
                </Pressable>
              )}

              {/* ⋯ 메뉴 팝오버 */}
              {isMenuOpen && (
                <View style={s.menu} pointerEvents="auto">
                  <Pressable onPress={onEdit} style={s.menuItem} android_ripple={{ color: '#eee' }}>
                    <Text style={s.menuText}>수정</Text>
                  </Pressable>
                  <View style={s.menuDivider} />
                  <Pressable onPress={onDelete} style={s.menuItem} android_ripple={{ color: '#fee' }}>
                    <Text style={[s.menuText, { color: '#FF3B30' }]}>삭제</Text>
                  </Pressable>
                </View>
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
        <View style={{ flex: 1, overflow: 'visible' }}>
          <FlatList<UComment>
            ref={listRef}
            data={flatComments}
            keyExtractor={(it) => String(it.id)}
            renderItem={renderComment}
            ListHeaderComponent={
              <View style={s.headerWrap}>
                <Text style={s.title}>{post.title}</Text>
                <Text style={s.meta}>
                  {post.authorNickname} · {new Date(post.createdAt).toLocaleString()}
                </Text>
                <Text style={s.content}>{post.content}</Text>
                <Text style={s.stats}>👀 {post.views}  ❤️ {post.likes}  💬 {post.commentCount}</Text>
                <View style={{ height: 16 }} />
                <Text style={s.section}>댓글</Text>
              </View>
            }
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
            removeClippedSubviews={false}                 // ✅ 팝오버 잘리기 방지
            keyboardShouldPersistTaps="handled"           // ✅ 터치 전달 보장
            // ✅ 열린 메뉴 셀만 zIndex 올려서 위에 뜨게
            CellRendererComponent={({ index, children, style, ...props }) => {
              const id = flatComments[index]?.id;
              const z = menuOpenId !== null && id === menuOpenId ? 9999 : 0;
              return (
                <View {...props} style={[style, { zIndex: z, elevation: z ? 20 : 0, overflow: 'visible' }]}>
                  {children}
                </View>
              );
            }}
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
  meta: { fontSize: 12, color: 'gray', marginBottom: 16 },
  content: { fontSize: 16, lineHeight: 22, color: '#222', marginBottom: 24 },
  stats: { fontSize: 12, color: '#888' },
  section: { fontSize: 16, fontWeight: '600', marginTop: 8 },
  footer: {
    position: 'absolute',
    left: 0, right: 0, bottom: 0,
    borderTopWidth: 1, borderColor: '#eee',
    backgroundColor: '#fff',
  },

  // ⋯ 팝오버 메뉴 (Android 터치 보장용 zIndex/elevation/visible)
  menu: {
    position: 'absolute',
    top: 0,
    right: 8,
    backgroundColor: '#fff',
    borderRadius: 10,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#ddd',
    shadowColor: '#000',
    shadowOpacity: 0.15,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 4 },
    elevation: 24,
    zIndex: 1000,
    overflow: 'hidden',
  },
  menuItem: { paddingVertical: 10, paddingHorizontal: 14, minWidth: 96 },
  menuText: { fontSize: 14, color: '#0A84FF', textAlign: 'left' },
  menuDivider: { height: StyleSheet.hairlineWidth, backgroundColor: '#eee' },
});
