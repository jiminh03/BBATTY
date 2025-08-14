import React, {
  useLayoutEffect,
  useMemo,
  useCallback,
  useRef,
  useState,
  useEffect,
} from 'react';
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
  Image,
  ScrollView,
} from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { HomeStackScreenProps } from '../../navigation/types';
import {
  usePostDetailQuery,
  useDeletePostMutation,
  usePostLikeActions,
} from '../../entities/post/queries/usePostQueries';
import { CommentForm } from '../../entities/comment/ui/CommentForm';
import { useUserStore } from '../../entities/user/model/userStore';
import {
  useCommentListQuery,
  useDeleteComment,
} from '../../entities/comment/queries/useCommentQueries';
import { useCommentStore } from '../../entities/comment/model/store';
import { CommentEditForm } from '../../entities/comment/ui/commentEditForm';
import { findTeamById } from '../../shared/team/teamTypes';

type Props = HomeStackScreenProps<'PostDetail'>;
const FORM_HEIGHT = 84;

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
      isDeleted:
        Number((c as any)?.is_deleted ?? (c as any)?.isDeleted ?? 0) === 1 ||
        !!(c as any)?.isDeleted,
      nickname: (c as any).nickname,
    };
    const children = expandNested((c as any).replies ?? [], (node.depth ?? depth) + 1, id);
    return [node, ...children];
  });
}

export default function PostDetailScreen({ route, navigation }: Props) {
  const postId = route.params.postId;

  // ✅ 모든 hook은 항상 최상단에서 호출
  const insets = useSafeAreaInsets();
  const { data: post, isLoading, isError, error } = usePostDetailQuery(postId);

  const myNickname = useUserStore((s) => s.currentUser?.nickname);

  const delPost = useDeletePostMutation();
  const { toggle, isBusy } = usePostLikeActions(postId);

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

  // 편집/삭제/팝오버 상태
  const [locallyDeleted, setLocallyDeleted] = useState<Set<number>>(new Set());
  const [preDeleteUpdatedAt, setPreDeleteUpdatedAt] = useState<Map<number, string>>(new Map());
  const [menuOpenId, setMenuOpenId] = useState<number | null>(null);
  const [headerMenuOpen, setHeaderMenuOpen] = useState(false);

  // 댓글 평탄화
  const rawParents = useMemo(
    () => (cmtPages?.pages ?? []).flatMap((p: any) => p?.comments ?? []),
    [cmtPages]
  );
  const flatComments = useMemo(() => expandNested(rawParents, 0, null), [rawParents]);

  const onEndReached = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) fetchNextPage();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  // 헤더(팀색 + 제목 + 햄버거)
  const teamColor =
    findTeamById(Number((post as any)?.teamId ?? 0))?.color ?? '#EE6B2F' /* 기본 주황 */;
  const headerTitle = post?.title ?? '게시글';

  const isMinePost = !!post && !!myNickname && post.authorNickname === myNickname;

  useLayoutEffect(() => {
  const truncateTitle = (text: string, maxLength: number) => {
    if (text.length <= maxLength) return text;
    return text.slice(0, maxLength) + '...';
  };

  navigation.setOptions({
    headerStyle: { backgroundColor: teamColor },
    headerTintColor: '#fff',
    headerTitleAlign: 'center',
   headerTitle: () => (
  <Text
    numberOfLines={1}
    ellipsizeMode="tail"
    style={{
      color: '#fff',
      fontWeight: '700',
      fontSize: 16,
      textAlign: 'center',
      // maxWidth: headerTitle.length > 8 ? '85%' : undefined // 길면만 제한
    }}
  >
    {headerTitle}
  </Text>
),

    headerRight: () => (
      isMinePost ? (
        <Pressable onPress={() => setHeaderMenuOpen((v) => !v)} hitSlop={10} style={{ padding: 8 }}>
          <Text style={{ color: '#fff', fontSize: 20 }}>≡</Text>
        </Pressable>
      ) : null
    ),
  });
}, [navigation, teamColor, headerTitle]);


  // 편집 중 대상이 사라지면 종료
  useEffect(() => {
    if (!editingCommentId) return;
    const t = flatComments.find((c) => String(c.id) === String(editingCommentId));
    if (!t) return;
    const gone = !!t.isDeleted || locallyDeleted.has(Number(t.id));
    if (gone) setEditingCommentId(null);
  }, [editingCommentId, flatComments, locallyDeleted, setEditingCommentId]);

  // 댓글 삭제(낙관)
  const handleDeleteComment = (id: number) => {
    const target = flatComments.find((c) => Number(c.id) === id);
    if (target?.updatedAt) setPreDeleteUpdatedAt((m) => new Map(m).set(id, target.updatedAt!));

    setMenuOpenId(null);
    setLocallyDeleted((prev) => new Set(prev).add(id));

    delComment.mutate(
      { commentId: id },
      {
        onError: () => {
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

  // 댓글 한 줄
  const renderComment = ({ item }: { item: UComment }) => {
    const indent = Number(item.depth ?? 0) * 12;
    const isTopLevel = (item.depth ?? 0) === 0;
    const nickname = item.authorNickname ?? item.nickname;

    const gone = !!item.isDeleted || locallyDeleted.has(Number(item.id));
    const isEditing = !gone && String(editingCommentId ?? '') === String(item.id);
    const isMineCmt =
      !!myNickname && (item.authorNickname === myNickname || item.nickname === myNickname);
    const isMenuOpen = isMineCmt && !gone && menuOpenId === item.id;

    const baseDate = item.updatedAt ?? item.createdAt;

    if (gone) {
      const displayWhen =
        preDeleteUpdatedAt.get(Number(item.id)) ?? item.updatedAt ?? item.createdAt;
      return (
        <View style={{ paddingHorizontal: 16, paddingVertical: 12, paddingLeft: 16 + indent }}>
          <Text style={{ fontWeight: '700', fontSize: 13 }}>{nickname ?? '알 수 없음'}</Text>
          <Text style={{ color: '#9AA0A6', fontSize: 11 }}>
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
      setReplyTarget?.({ id: Number(item.id), nickname: nickname ?? '' });
      requestAnimationFrame(() => listRef.current?.scrollToEnd({ animated: true }));
    };
    const onEdit = () => {
      setMenuOpenId(null);
      setEditingCommentId(String(item.id));
    };
    const onDelete = () => handleDeleteComment(Number(item.id));

    return (
      <View
        style={{
          paddingHorizontal: 16,
          paddingVertical: 12,
          paddingLeft: 16 + indent,
          overflow: 'visible',
        }}
        collapsable={false}
      >
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <View style={{ flex: 1 }}>
            <Text style={{ fontWeight: '700', fontSize: 13 }}>{nickname}</Text>
            <Text style={{ color: '#9AA0A6', fontSize: 11 }}>
              {baseDate ? new Date(baseDate).toLocaleString() : ''}
            </Text>
          </View>

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
          <CommentEditForm
            postId={postId}
            commentId={Number(item.id)}
            initialContent={item.content ?? ''}
          />
        ) : (
          <>
            <View style={{ marginTop: 6 }}>
              <Text style={{ fontSize: 14, color: '#222' }}>{item.content}</Text>
            </View>

            <View style={{ marginTop: 8 }}>
              {isTopLevel && (
                <Pressable
                  onPress={onReply}
                  style={{
                    alignSelf: 'flex-start',
                    paddingVertical: 6,
                    paddingHorizontal: 10,
                    borderRadius: 10,
                    backgroundColor: '#F5F6F7',
                  }}
                >
                  <Text style={{ color: '#0A84FF', fontWeight: '600', fontSize: 12 }}>
                    답글 달기
                  </Text>
                </Pressable>
              )}

              {isMenuOpen && (
                <View style={s.menu} pointerEvents="auto">
                  <Pressable onPress={onEdit} style={s.menuItem}>
                    <Text style={s.menuText}>수정</Text>
                  </Pressable>
                  <View style={s.menuDivider} />
                  <Pressable onPress={onDelete} style={s.menuItem}>
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

  // 본문 파생값
  const viewCount = (post as any)?.views ?? (post as any)?.viewCount ?? 0;
  const likeCount = (post as any)?.likes ?? (post as any)?.likeCount ?? 0;
  const cmtCount = (post as any)?.commentCount ?? (post as any)?.commentsCount ?? 0;

  const images: string[] = Array.isArray((post as any)?.images) ? (post as any).images : [];

  // ====== JSX 분기(로딩/에러/정상) — hook 호출 뒤에만 분기 ======
  const Body =
    isLoading ? (
      <View style={s.center}>
        <ActivityIndicator size="large" />
      </View>
    ) : isError || !post ? (
      <View style={s.center}>
        <Text>{isError ? `오류: ${(error as Error).message}` : '게시글을 찾을 수 없어요.'}</Text>
      </View>
    ) : (
      <View style={{ flex: 1, overflow: 'visible' }}>
        {/* 상단 햄버거 메뉴 (헤더에서 토글) */}
        {headerMenuOpen && (
          <View
            pointerEvents="box-none"
            style={{
              position: 'absolute',
              top: insets.top + 6,
              right: 8,
              zIndex: 9999,
            }}
          >
            <View style={s.headerMenu}>
              {isMinePost ? (
                <>
                  <Pressable
                    onPress={() => {
                      setHeaderMenuOpen(false);
                      navigation.navigate('PostForm', { postId });
                    }}
                    style={s.headerMenuItem}
                  >
                    <Text style={s.headerMenuText}>수정</Text>
                  </Pressable>
                  <View style={s.menuDivider} />
                  <Pressable
                    onPress={() => {
                      setHeaderMenuOpen(false);
                      Alert.alert('삭제', '정말 삭제할까요?', [
                        { text: '취소', style: 'cancel' },
                        {
                          text: '삭제',
                          style: 'destructive',
                          onPress: () =>
                            delPost.mutate(postId, { onSuccess: () => navigation.goBack() }),
                        },
                      ]);
                    }}
                    style={s.headerMenuItem}
                  >
                    <Text style={[s.headerMenuText, { color: '#FF3B30' }]}>삭제</Text>
                  </Pressable>
                </>
              ) : (
                <Pressable
                  onPress={() => setHeaderMenuOpen(false)}
                  style={s.headerMenuItem}
                >
                  <Text style={s.headerMenuText}>닫기</Text>
                </Pressable>
              )}
            </View>
          </View>
        )}

        <FlatList<UComment>
          ref={listRef}
          data={flatComments}
          keyExtractor={(it) => String(it.id)}
          renderItem={renderComment}
          ListHeaderComponent={
            <View style={s.headerCard}>
              {/* 작성자/시간/조회수 */}
              <View style={s.authorRow}>
                <View style={s.avatar} />
                <View style={{ flex: 1 }}>
                  <Text style={s.authorName}>{post.authorNickname}</Text>
                  <Text style={s.authorMeta}>
                    {new Date(post.createdAt).toLocaleString()} · 👁 {viewCount}
                  </Text>
                </View>
              </View>

              {/* 제목 */}
              <Text style={s.title}>{post.title}</Text>

              {/* 본문 */}
              <View style={{ marginTop: 8 }}>
                {(post.content ?? '')
                  .split(/\n+/)
                  .filter(Boolean)
                  .map((line, idx) => (
                    <Text key={idx} style={s.paragraph}>
                      {line}
                    </Text>
                  ))}
              </View>

              {/* 이미지 카드(있을 때만) */}
              {images.length > 0 && (
                <ScrollView
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  contentContainerStyle={{ gap: 10, paddingTop: 12 }}
                >
                  {images.map((uri, i) => (
                    <Image key={i} source={{ uri }} style={s.imageCard} resizeMode="cover" />
                  ))}
                </ScrollView>
              )}

              {/* 통계 + 좋아요 */}
              <View style={s.statsRow}>
                <Pressable onPress={toggle} disabled={isBusy} hitSlop={10} style={s.likeBtn}>
                  <Text style={s.likeIcon}>{(post as any)?.likedByMe ? '❤️' : '🤍'}</Text>
                  <Text style={s.likeCount}>{likeCount}</Text>
                </Pressable>
                <View style={{ width: 12 }} />
                <Text style={s.stats}>💬 {cmtCount}</Text>
              </View>

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
          removeClippedSubviews={false}
          keyboardShouldPersistTaps="handled"
          CellRendererComponent={({ index, children, style, ...props }) => {
            const id = flatComments[index]?.id;
            const z = headerMenuOpen || (menuOpenId !== null && id === menuOpenId) ? 9999 : 0;
            return (
              <View
                {...props}
                style={[style, { zIndex: z, elevation: z ? 20 : 0, overflow: 'visible' }]}
              >
                {children}
              </View>
            );
          }}
        />

        {/* 하단 댓글 입력 */}
        <CommentForm
          postId={postId}
          style={[s.footer, { paddingBottom: Math.max(12, insets.bottom) }]}
        />
      </View>
    );

  return (
    <KeyboardAvoidingView
      style={{ flex: 1, backgroundColor: '#fff' }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? insets.top : 0}
    >
      <SafeAreaView style={{ flex: 1 }}>{Body}</SafeAreaView>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#fff' },

  // 본문 카드 영역
  headerCard: {
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 12,
    backgroundColor: '#fff',
  },

  authorRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
  avatar: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#EDEFF1', marginRight: 10 },
  authorName: { fontWeight: '700', color: '#111', fontSize: 14 },
  authorMeta: { color: '#9AA0A6', fontSize: 12, marginTop: 2 },

  title: { fontSize: 20, fontWeight: '800', color: '#111', marginTop: 4 },

  paragraph: { fontSize: 15, lineHeight: 22, color: '#222', marginTop: 6 },

  imageCard: {
    width: 260,
    height: 160,
    borderRadius: 12,
    backgroundColor: '#F2F3F4',
  },

  statsRow: { flexDirection: 'row', alignItems: 'center', marginTop: 14, marginBottom: 10 },
  likeBtn: { flexDirection: 'row', alignItems: 'center' },
  likeIcon: { fontSize: 20, marginRight: 6 },
  likeCount: { fontSize: 13, color: '#222' },
  stats: { fontSize: 12, color: '#888' },

  section: { fontSize: 16, fontWeight: '700', marginTop: 12 },

  footer: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    borderTopWidth: 1,
    borderColor: '#eee',
    backgroundColor: '#fff',
  },

  // 댓글 행 메뉴(⋯)
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

  // 헤더 햄버거 메뉴(≡)
  headerMenu: {
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#ddd',
    paddingVertical: 6,
    minWidth: 120,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 6 },
    elevation: 30,
  },
  headerMenuItem: { paddingVertical: 10, paddingHorizontal: 14 },
  headerMenuText: { fontSize: 14, color: '#111', fontWeight: '600' },
});
