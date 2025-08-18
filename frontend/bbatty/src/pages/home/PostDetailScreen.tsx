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
  StatusBar,
} from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { useFocusEffect } from '@react-navigation/native';
import { HomeStackScreenProps } from '../../navigation/types';
import { useTabBar } from '../../shared/contexts/TabBarContext';
import {
  usePostDetailQuery,
  useDeletePostMutation,
  usePostLikeActions,
} from '../../entities/post/queries/usePostQueries';
import { CommentForm, FORM_MIN_HEIGHT } from '../../entities/comment/ui/CommentForm';
import { useUserStore } from '../../entities/user/model/userStore';
import {
  useCommentListQuery,
  useDeleteComment,
} from '../../entities/comment/queries/useCommentQueries';
import { useCommentStore } from '../../entities/comment/model/store';
import { CommentEditForm } from '../../entities/comment/ui/commentEditForm';
import { findTeamById } from '../../shared/team/teamTypes';
import { useRem, useMinTouch } from '../../shared/ui/atoms/button/responsive';

type Props = HomeStackScreenProps<'PostDetail'>;

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

function timeOf(n: any): number {
  const t = n?.createdAt ?? n?.updatedAt ?? n?.created_at ?? n?.updated_at;
  const ts = t ? new Date(t).getTime() : NaN;
  if (Number.isFinite(ts)) return ts;
  const idNum = typeof n?.id === 'number' ? n.id : Number(n?.id ?? 0);
  return Number.isFinite(idNum) ? idNum : 0;
}

function sortTreeAsc(list: any[] | undefined | null): any[] {
  const arr = Array.isArray(list) ? [...list] : [];
  arr.sort((a, b) => timeOf(a) - timeOf(b));
  return arr.map((node) => {
    const replies = Array.isArray((node as any).replies)
      ? sortTreeAsc((node as any).replies)
      : [];
    return { ...(node as object), replies } as any;
  });
}

function expandNested(list: any[], depth = 0, parentId: number | null = null): UComment[] {
  if (!Array.isArray(list)) return [];
  return list.flatMap((c) => {
    const id = Number(c.id ?? c.commentId);
    const node: UComment = {
      ...(c as object),
      id,
      depth,
      parentId,
      isDeleted:
        Number((c as any)?.is_deleted ?? (c as any)?.isDeleted ?? 0) === 1 ||
        !!(c as any)?.isDeleted,
      nickname: (c as any).nickname,
    };
    const children = expandNested((c as any).replies ?? [], depth + 1, id);
    return [node, ...children];
  });
}

export default function PostDetailScreen({ route, navigation }: Props) {
  const rem = useRem();
  const minTouch = useMinTouch();
  const postId = route.params.postId;
  const fromProfile = route.params?.fromProfile || false;
  const insets = useSafeAreaInsets();
  const { setTabBarVisible } = useTabBar();

  // 프로필에서 온 경우 tab bar 숨기기
  useFocusEffect(
    React.useCallback(() => {
      if (fromProfile) {
        console.log('PostDetail - 프로필에서 접근, 탭바 숨김');
        setTabBarVisible(false);
      }

      return () => {
        if (fromProfile) {
          console.log('PostDetail - 프로필로 돌아가면서 탭바 복원');
          setTabBarVisible(true);
        }
      };
    }, [fromProfile, setTabBarVisible])
  );

  const { data: post, isLoading, isError, error } = usePostDetailQuery(postId, { refetchOnFocus: true });

  const myNickname = useUserStore((s) => s.currentUser?.nickname);
  const userTeamId = useUserStore((s) => s.currentUser?.teamId) ?? 0;

  const delPost = useDeletePostMutation();

  const rawTeamId = (post as any)?.teamId ?? route.params?.teamId ?? userTeamId ?? 0;
  const teamId = Number(rawTeamId) || 0;
  const teamColor = findTeamById(teamId)?.color ?? '#222222';

  const canComment = !!post && teamId === userTeamId;
  const canLike = canComment;

  const { toggle, isBusy } = usePostLikeActions(postId, {
     teamId: teamId || undefined,
     // refetchAfterMs: 0,
   });

  const {
    data: cmtPages,
    isLoading: cLoading,
    isError: cError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useCommentListQuery(postId, 10);

  const delComment = useDeleteComment(postId);
  const editingCommentId    = useCommentStore((s) => s.editingCommentId);
  const setEditingCommentId = useCommentStore((s) => s.setEditingCommentId);
  const setReplyTarget      = useCommentStore((s) => s.setReplyTarget);

  const listRef = useRef<FlatList<UComment>>(null);

  const [locallyDeleted, setLocallyDeleted] = useState<Set<number>>(new Set());
  const [preDeleteUpdatedAt, setPreDeleteUpdatedAt] = useState<Map<number, string>>(new Map());
  const [menuOpenId, setMenuOpenId] = useState<number | null>(null);
  const [headerMenuOpen, setHeaderMenuOpen] = useState(false);

  const rawParents = useMemo(
    () => (cmtPages?.pages ?? []).flatMap((p: any) => p?.comments ?? []),
    [cmtPages]
  );

  const sortedParents = useMemo(() => sortTreeAsc(rawParents), [rawParents]);
  const flatComments = useMemo(() => expandNested(sortedParents, 0, null), [sortedParents]);

  const prevCountRef = useRef<number>(0);
  useEffect(() => {
    const count = flatComments.length;
    if (count > prevCountRef.current) {
      requestAnimationFrame(() => listRef.current?.scrollToEnd({ animated: true }));
    }
    prevCountRef.current = count;
  }, [flatComments.length]);

  const onEndReached = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) fetchNextPage();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const headerTitle = post?.title ?? '게시글';
  const isMinePost = !!post && !!myNickname && post.authorNickname === myNickname;

  const authorAvatarUrl =
  (post as any)?.profileImg ??    // ✅ 여기에 추가
  (post as any)?.authorProfileUrl ??
  (isMinePost
    ? ((useUserStore.getState().currentUser as any)?.profileImg ??   // ✅ 여기도 맞춰줌
      (useUserStore.getState().currentUser as any)?.profileImageUrl ??
      (useUserStore.getState().currentUser as any)?.profileUrl ??
      (useUserStore.getState().currentUser as any)?.avatarUrl ??
      undefined)
    : undefined);


  const handleGoBack = useCallback(() => {
    navigation.goBack();
  }, [navigation]);

  useLayoutEffect(() => {
    navigation.setOptions({
      headerShown: true,
      headerStyle: { backgroundColor: teamColor },
      headerTintColor: '#fff',
      headerTitleAlign: 'center',
      headerLeft: () => (
        <Pressable
          onPress={handleGoBack}
          hitSlop={8}
          style={{ padding: Math.max(8, (minTouch - 32) / 2), marginLeft: 8 }}
        >
          <Text style={{ color: '#fff', fontSize: rem(1.125), fontWeight: '600' }}>←</Text>
        </Pressable>
      ),
      headerTitle: () => (
        <Text
          numberOfLines={1}
          ellipsizeMode="tail"
          style={{ color: '#fff', fontWeight: '700', fontSize: rem(1), textAlign: 'center' }}
        >
          {headerTitle}
        </Text>
      ),
      headerRight: () =>
        isMinePost ? (
          <Pressable
            onPress={() => setHeaderMenuOpen((v) => !v)}
            hitSlop={8}
            style={{ padding: Math.max(8, (minTouch - 30) / 2), marginRight: 6 }}
          >
            <Text style={{ color: '#fff', fontSize: rem(1.5), lineHeight: rem(1.5) }}>☰</Text>
          </Pressable>
        ) : null,
    });
  }, [navigation, teamColor, headerTitle, isMinePost, handleGoBack, rem, minTouch]);

  useEffect(() => {
    if (Platform.OS === 'android') {
      StatusBar.setBackgroundColor(teamColor);
    }
    StatusBar.setBarStyle('light-content');
  }, [teamColor]);

  useEffect(() => {
    if (!editingCommentId) return;
    const t = flatComments.find((c) => String(c.id) === String(editingCommentId));
    if (!t) return;
    const gone = !!t.isDeleted || locallyDeleted.has(Number(t.id));
    if (gone) setEditingCommentId(null);
  }, [editingCommentId, flatComments, locallyDeleted, setEditingCommentId]);

  const handleDeleteComment = (id: number) => {
    const target = flatComments.find((c) => Number(c.id) === id);
    if (target?.updatedAt)
      setPreDeleteUpdatedAt((m) => new Map(m).set(id, target.updatedAt!));

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

  const viewCount = post?.views ?? 0;
  const likeCount = (typeof post?.likes === 'number'
    ? post?.likes
    : (post as any)?.likeCount) ?? 0;
  const cmtCount = flatComments.length;
  const liked = !!post?.isLiked;

  const images: string[] = Array.isArray(post?.images) ? (post!.images as string[]) : [];

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
        {headerMenuOpen && (
          <View pointerEvents="box-none" style={{ position: 'absolute', top: -20, right: 8, zIndex: 9999 }}>
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
                            delPost.mutate(postId, {
                              onSuccess: () => navigation.goBack(),
                            }),
                        },
                      ]);
                    }}
                    style={s.headerMenuItem}
                  >
                    <Text style={[s.headerMenuText, { color: '#FF3B30' }]}>삭제</Text>
                  </Pressable>
                </>
              ) : (
                <Pressable onPress={() => setHeaderMenuOpen(false)} style={s.headerMenuItem}>
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
          renderItem={({ item }) => {
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
                    {displayWhen ? new Date(displayWhen).toLocaleString('ko-KR', {
                      timeZone: 'Asia/Seoul',
                      year: 'numeric',
                      month: '2-digit',
                      day: '2-digit',
                      hour: '2-digit',
                      minute: '2-digit',
                    }) : ''}
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
                      {baseDate ? new Date(baseDate).toLocaleString('ko-KR', {
                        timeZone: 'Asia/Seoul',
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                      }) : ''}
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
                      {isTopLevel && canComment && (
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
                          <Text style={{ color: teamColor, fontWeight: '600', fontSize: 12 }}>
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
          }}
          ListHeaderComponent={
            <View style={s.headerCard}>
              <View style={s.authorRow}>
                {authorAvatarUrl ? (
                  <Image source={{ uri: authorAvatarUrl }} style={s.avatar} />
                ) : (
                  <View style={s.avatar} />
                )}
                <View style={{ flex: 1 }}>
                  <Text style={s.authorName}>{post!.authorNickname}</Text>
                  <Text style={s.authorMeta}>
                    {new Date(post!.createdAt).toLocaleString('ko-KR', {
                      timeZone: 'Asia/Seoul',
                      year: 'numeric',
                      month: '2-digit',
                      day: '2-digit',
                      hour: '2-digit',
                      minute: '2-digit',
                    })} · 👁 {viewCount}
                  </Text>
                </View>
              </View>

              <Text style={s.title}>{post!.title}</Text>

              <View style={{ marginTop: 8 }}>
                {(post!.content ?? '')
                  .split(/\n+/)
                  .filter(Boolean)
                  .map((line, idx) => {
                    // 마크다운 이미지 패턴 검사: ![image](imageUrl)
                    const imageMatch = line.match(/^!\[image\]\((.+)\)$/);
                    
                    if (imageMatch) {
                      const imageUrl = imageMatch[1];
                      return (
                        <Image 
                          key={idx} 
                          source={{ uri: imageUrl }} 
                          style={s.contentImage} 
                          resizeMode="cover" 
                        />
                      );
                    }
                    
                    return (
                      <Text key={idx} style={s.paragraph}>
                        {line}
                      </Text>
                    );
                  })}
              </View>

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

              <View style={s.statsRow}>
                {canLike ? (
                  <Pressable onPress={toggle} hitSlop={10} style={s.likeBtn}>
                    <Text style={s.likeIcon}>{liked ? '❤️' : '🤍'}</Text>
                    <Text style={s.likeCount}>{likeCount}</Text>
                  </Pressable>
                ) : (
                  <View style={[s.likeBtn, { opacity: 0.4 }]}>
                    <Text style={s.likeIcon}>🤍</Text>
                    <Text style={s.likeCount}>{likeCount}</Text>
                  </View>
                )}
                <View style={{ width: 12 }} />
                <Text style={s.stats}>💬 {cmtCount}</Text>
              </View>
            </View>
          }
          ListEmptyComponent={
            cLoading ? (
              <ActivityIndicator style={{ margin: 16 }} />
            ) : cError ? (
              <Text style={{ margin: 16 }}>댓글을 불러오는 데 실패했습니다.</Text>
            ) : (
              <Text style={{ margin: 16, color: '#777' }}>
                {canComment ? '첫 댓글을 남겨보세요.' : '댓글이 없습니다.'}
              </Text>
            )
          }
          onEndReachedThreshold={0.4}
          onEndReached={onEndReached}
          ListFooterComponent={
            isFetchingNextPage ? <ActivityIndicator style={{ marginVertical: 12 }} /> : <View />
          }
          contentContainerStyle={{
            paddingBottom: (canComment ? FORM_MIN_HEIGHT : 0) + insets.bottom + 12,
          }}
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
      </View>
    );

  return (
  <SafeAreaView style={{ flex: 1, backgroundColor: '#fff' }}>
    <StatusBar backgroundColor={teamColor} barStyle="light-content" />

    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={insets.top} // 헤더 높이 고려
    >
      {/* 댓글 리스트 영역 */}
      {Body}

      {/* 댓글 입력창을 Footer가 아닌 화면 맨 아래 고정 */}
      <CommentForm
        postId={postId}
        teamColor={teamColor}
        enabled={canComment}
        style={{ paddingBottom: insets.bottom }}
      />
    </KeyboardAvoidingView>
  </SafeAreaView>
);
}

const s = StyleSheet.create({
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#fff' },
  headerCard: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 12, backgroundColor: '#fff' },
  authorRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
  avatar: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#EDEFF1', marginRight: 10 },
  authorName: { fontWeight: '700', color: '#111', fontSize: 14 },
  authorMeta: { color: '#9AA0A6', fontSize: 12, marginTop: 2 },
  title: { fontSize: 20, fontWeight: '800', color: '#111', marginTop: 4 },
  paragraph: { fontSize: 15, lineHeight: 22, color: '#222', marginTop: 6 },
  contentImage: { width: '100%', height: 200, borderRadius: 12, backgroundColor: '#F2F3F4', marginTop: 8 },
  imageCard: { width: 260, height: 160, borderRadius: 12, backgroundColor: '#F2F3F4' },
  statsRow: { flexDirection: 'row', alignItems: 'center', marginTop: 14, marginBottom: 10 },
  likeBtn: { flexDirection: 'row', alignItems: 'center' },
  likeIcon: { fontSize: 20, marginRight: 6 },
  likeCount: { fontSize: 13, color: '#222' },
  stats: { fontSize: 12, color: '#888' },
  section: { fontSize: 16, fontWeight: '700', marginTop: 12 },
  menu: {
    position: 'absolute', top: -40, right: 8,
    backgroundColor: '#fff', borderRadius: 10,
    borderWidth: StyleSheet.hairlineWidth, borderColor: '#ddd',
    shadowColor: '#000', shadowOpacity: 0.15, shadowRadius: 8,
    shadowOffset: { width: 0, height: 4 }, elevation: 24, zIndex: 1000,
    overflow: 'hidden',
  },
  menuItem: { paddingVertical: 10, paddingHorizontal: 14, minWidth: 96 },
  menuText: { fontSize: 14, color: '#0A84FF', textAlign: 'left' },
  menuDivider: { height: StyleSheet.hairlineWidth, backgroundColor: '#eee' },
  headerMenu: {
    backgroundColor: '#fff', borderRadius: 12,
    borderWidth: StyleSheet.hairlineWidth, borderColor: '#ddd',
    paddingVertical: 6, minWidth: 120,
    shadowColor: '#000', shadowOpacity: 0.2, shadowRadius: 10,
    shadowOffset: { width: 0, height: 6 }, elevation: 30,
  },
  headerMenuItem: { paddingVertical: 10, paddingHorizontal: 14 },
  headerMenuText: { fontSize: 14, color: '#111', fontWeight: '600' },
});
