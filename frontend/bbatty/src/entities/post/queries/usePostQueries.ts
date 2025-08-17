import {
  useMutation,
  useInfiniteQuery,
  useQueryClient,
  useQuery,
  InfiniteData,
} from '@tanstack/react-query';
import { useCallback, useRef, useEffect } from 'react';
import { postApi, TeamNewsItem } from '../api/api';
import { CreatePostPayload, CursorPostListResponse, PostListItem } from '../api/types';
import { Post } from '../model/types';
import { useLikeStore } from '../model/store';
import { useUserStore } from '../../user/model/userStore';

const idNum = (x: any) =>
  typeof x?.id === 'number' ? x.id : Number(x?.id ?? 0) || 0;

/* ======================== 목록 ======================== */
export const usePostListQuery = (teamId: number) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['posts', teamId],
    queryFn: async ({ pageParam }) => {
      const res = await postApi.getPosts(
        teamId,
        typeof pageParam === 'string'
          ? Number(pageParam)
          : (pageParam as number | undefined),
      );

      // ✅ 응답 정규화
      return {
        ...res,
        posts: (res.posts ?? []).map((p: any) => ({
          ...p,
          // 좋아요 관련
          likes:
            typeof p.likes === 'number'
              ? p.likes
              : typeof p.likeCount === 'number'
              ? p.likeCount
              : 0,
          isLiked:
            typeof p.isLiked === 'boolean'
              ? p.isLiked
              : typeof p.likedByMe === 'boolean'
              ? p.likedByMe
              : !!p.liked,

          // 댓글 수 관련
          commentCount:
            typeof p.commentCount === 'number'
              ? p.commentCount
              : typeof p.commentsCount === 'number'
              ? p.commentsCount
              : (p as any).commentCnt ?? 0,

          // 조회수 관련
          views:
            typeof p.views === 'number'
              ? p.views
              : typeof p.viewCount === 'number'
              ? p.viewCount
              : 0,
        })),
      };
    },
    initialPageParam: undefined,
    getNextPageParam: (last) =>
      !last?.hasNext
        ? undefined
        : last.nextCursor ??
          (last.posts?.length
            ? idNum(last.posts[last.posts.length - 1])
            : undefined),
  });

export const useCreatePost = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreatePostPayload) => postApi.createPost(payload),
    onSuccess: (_r, vars) =>
      qc.invalidateQueries({ queryKey: ['posts', (vars as any).teamId] }),
  });
};

/* ================= 상세(서버우선 + 스토어 fallback) ================= */
export const usePostDetailQuery = (
  postId: number | null,
  opts?: { refetchOnFocus?: boolean },
) => {
  const userId =
    useUserStore(
      (s: any) => s.currentUser?.id ?? s.currentUser?.userId ?? null,
    ) ?? null;

  useEffect(() => {
    useLikeStore.getState().setSessionUser(userId);
  }, [userId]);

  const getLiked = useLikeStore((s) => s.getLiked);

  return useQuery<Post>({
    queryKey: ['post', postId],
    queryFn: () => postApi.getPostById(postId!),
    enabled: postId !== null,
    select: (p) => {
      const local = getLiked(postId!, userId);

      const serverLikedRaw =
        typeof (p as any).isLiked === 'boolean'
          ? (p as any).isLiked
          : typeof (p as any).likedByMe === 'boolean'
          ? (p as any).likedByMe
          : typeof (p as any).liked === 'boolean'
          ? (p as any).liked
          : undefined;

      const isLiked =
        typeof local === 'boolean'
          ? local
          : typeof serverLikedRaw === 'boolean'
          ? serverLikedRaw
          : false;

      const safeAuthor = p.authorNickname ?? (p as any).nickname ?? '탈퇴한 사용자';

      // ✅ 여기서 likes 계산을 보정
      const rawLikes =
        typeof (p as any).likes === 'number'
          ? (p as any).likes
          : typeof (p as any).likeCount === 'number'
          ? (p as any).likeCount
          : 0;

      const safeLikes = rawLikes + (isLiked ? 0 : 0); // 필요하면 보정 로직 추가

      return {
        ...p,
        authorNickname: safeAuthor,
        isLiked,
        likes: safeLikes,
        likeCount: safeLikes,
      } as Post;
    },
    refetchOnWindowFocus: opts?.refetchOnFocus ?? true,
    staleTime: 3000,
    placeholderData: (prev) => prev,
  });
};

/* ===================== 삭제/수정 ===================== */
export const useDeletePostMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (postId: number) => postApi.deletePost(postId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['posts'] }),
  });
};

export const useUpdatePost = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: {
      postId: number;
      title: string;
      content: string;
      teamId?: number;
      status?: string;
      postStatus?: string;
    }) => postApi.updatePost(vars.postId, vars),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: ['post', vars.postId] });
      if (vars.teamId) qc.invalidateQueries({ queryKey: ['posts', vars.teamId] });
    },
  });
};

/* =================== 목록 전파 유틸 =================== */
// 모든 별칭까지 일괄 반영
function patchPostViews(post: any, postId: number, delta: number) {
  const base = post.views ?? post.viewCount ?? 0;
  return { ...post, views: base + delta, viewCount: base + delta };
}

export function syncViewEverywhere(qc: ReturnType<typeof useQueryClient>, postId: number, delta = 1) {
  const patchInfinite = (old?: InfiniteData<CursorPostListResponse>) => {
    if (!old) return old;
    return {
      ...old,
      pages: old.pages.map((pg) => ({
        ...pg,
        posts: (pg.posts ?? []).map((p: any) => patchPostViews(p, postId, delta)),
      })),
    };
  };

  qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
    { queryKey: ['posts'] },
    patchInfinite,
  );
}



function patchPost(post: any, postId: number, liked?: boolean, likeDelta?: number) {
  if (String(post?.id) !== String(postId)) return post;
  const base =
    typeof post.likes === 'number'
      ? post.likes
      : typeof post.likeCount === 'number'
      ? post.likeCount
      : 0;
  const nextLikes =
    typeof likeDelta === 'number' ? Math.max(0, base + likeDelta) : base;

  const next = { ...post };
  if (typeof liked === 'boolean') {
    next.isLiked = liked;
    next.likedByMe = liked;
    next.liked = liked;
  }
  next.likes = nextLikes;
  next.likeCount = nextLikes;
  return next;
}

export function syncLikeEverywhere(
  qc: ReturnType<typeof useQueryClient>,
  postId: number,
  {
    liked,
    likeDelta,
    teamId,
    userId,
    keyword,
  }: {
    liked?: boolean;
    likeDelta?: number;
    teamId?: number;
    userId?: number | null;
    keyword?: string;
  },
) {
  // 1) 상세
  qc.setQueryData(['post', postId], (old: any) =>
    old ? patchPost(old, postId, liked, likeDelta) : old,
  );

  // 공통 무한쿼리 패처
  const patchInfinite = (old?: InfiniteData<CursorPostListResponse>) => {
    if (!old) return old;
    return {
      ...(old as any),
      pages: old.pages.map((pg) => ({
        ...(pg as any),
        posts: (pg.posts ?? []).map((p: any) =>
          patchPost(p, postId, liked, likeDelta),
        ),
      })),
    } as any;
  };

  // 2) 팀 목록/인기
  if (typeof teamId === 'number') {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['posts', teamId] },
      patchInfinite,
    );
    qc.setQueriesData<any[]>(
      { queryKey: ['popularPostsAll', teamId] },
      (old) =>
        old ? (old as any).map((p: any) => patchPost(p, postId, liked, likeDelta)) : old,
    );
    qc.setQueriesData<any[]>(
      { queryKey: ['popularPostsPreview', teamId] },
      (old) =>
        old ? (old as any).map((p: any) => patchPost(p, postId, liked, likeDelta)) : old,
    );
  } else {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['posts'] },
      patchInfinite,
    );
    qc.setQueriesData<any[]>(
      { queryKey: ['popularPostsAll'] },
      (old) =>
        old ? (old as any).map((p: any) => patchPost(p, postId, liked, likeDelta)) : old,
    );
    qc.setQueriesData<any[]>(
      { queryKey: ['popularPostsPreview'] },
      (old) =>
        old ? (old as any).map((p: any) => patchPost(p, postId, liked, likeDelta)) : old,
    );
  }

  // 3) 내 글
  qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
    typeof userId === 'number' ? { queryKey: ['myPosts', userId] } : { queryKey: ['myPosts'] },
    patchInfinite,
  );

  // 4) 검색
  if (typeof teamId === 'number' && typeof keyword === 'string') {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['searchPosts', teamId, keyword] },
      patchInfinite,
    );
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['teamSearch', teamId, keyword] },
      patchInfinite,
    );
  } else {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['searchPosts'] },
      patchInfinite,
    );
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['teamSearch'] },
      patchInfinite,
    );
  }
}

/* =================== 좋아요 토글 =================== */
export const usePostLikeActions = (
  postId: number,
  options?: {
    teamId?: number;
    listKeyword?: string;
    onRequireLogin?: () => void;
    debounceMs?: number; // 기본 500
    refetchAfterMs?: number; // 기본 300
  },
) => {
  const qc = useQueryClient();
  const userId =
    useUserStore(
      (s: any) => s.currentUser?.id ?? s.currentUser?.userId ?? null,
    ) ?? null;
  const teamId = options?.teamId;
  const keyword = options?.listKeyword;
  const DEBOUNCE_MS = options?.debounceMs ?? 500;

  useEffect(() => {
    useLikeStore.getState().setSessionUser(userId);
  }, [userId]);

  const setLikedStore = useLikeStore((s) => s.setLiked);
  const detailKey = ['post', postId] as const;

  /** 서버에 마지막으로 동기화된 liked */
  const serverLikedRef = useRef<boolean | null>(null);
  /** 사용자가 원하는 최종 liked */
  const desiredLikedRef = useRef<boolean | null>(null);
  /** 서버 likes 기준값 */
  const serverLikesRef = useRef<number | null>(null);
  /** 서버 요청 진행 중 여부 */
  const inFlightRef = useRef(false);
  /** 디바운스 타이머 */
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 최초 서버 liked/likes 값 주입
  useEffect(() => {
    const curr = qc.getQueryData<Post>(detailKey) as Post | undefined;
    const liked =
      typeof curr?.isLiked === 'boolean'
        ? curr.isLiked
        : typeof (curr as any)?.likedByMe === 'boolean'
        ? (curr as any).likedByMe
        : !!(curr as any)?.liked;
    const likes =
      typeof curr?.likes === 'number'
        ? curr.likes
        : typeof (curr as any)?.likeCount === 'number'
        ? (curr as any).likeCount
        : 0;
    serverLikedRef.current = liked;
    desiredLikedRef.current = liked;
    // ✅ 서버 likes 기준값 = 총 좋아요 - (내가 눌렀으면 1 빼고 저장)
    serverLikesRef.current = likes - (liked ? 1 : 0);
  }, [qc, postId]);

  /** 낙관 반영 (누적 X, 항상 서버 기준값 + 내 상태) */
  const applyOptimistic = (liked: boolean) => {
    setLikedStore(postId, liked, userId);
    qc.setQueryData<Post>(detailKey, (old) => {
      if (!old) return old as any;
      const serverBase = serverLikesRef.current ?? 0;
      const safeLikes = serverBase + (liked ? 1 : 0);
      return {
        ...(old as any),
        isLiked: liked,
        likes: safeLikes,
        likeCount: safeLikes,
      } as Post;
    });
    syncLikeEverywhere(qc, postId, { liked, likeDelta: 0, teamId, userId, keyword });
  };

  /** 서버로 실제 동기화 */
  const flush = useCallback(() => {
    if (inFlightRef.current) return;
    const desired = desiredLikedRef.current;
    const server = serverLikedRef.current;
    if (desired === null || server === null || desired === server) return;

    inFlightRef.current = true;
    const call = desired ? postApi.likePost : postApi.unlikePost;
    call(postId)
      .then(() => {
        serverLikedRef.current = desired;
        // serverLikesRef.current 그대로 유지
      })
      .catch(() => {
        qc.invalidateQueries({ queryKey: detailKey });
        serverLikedRef.current = null;
        serverLikesRef.current = null;
      })
      .finally(() => {
        inFlightRef.current = false;
        if (timerRef.current) clearTimeout(timerRef.current);
        timerRef.current = setTimeout(() => {
          timerRef.current = null;
          flush();
        }, DEBOUNCE_MS);
      });
  }, [postId, qc, DEBOUNCE_MS]);

  /** 디바운스 스케줄 */
  const scheduleFlush = () => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => {
      timerRef.current = null;
      flush();
    }, DEBOUNCE_MS);
  };

  /** 사용자 탭 */
  const toggle = useCallback(() => {
    if (!userId) {
      options?.onRequireLogin?.();
      return;
    }
    const curr = qc.getQueryData<Post>(detailKey) as Post | undefined;
    const currLiked =
      curr?.isLiked ??
      (typeof (curr as any)?.likedByMe === 'boolean'
        ? (curr as any).likedByMe
        : !!(curr as any)?.liked);
    const nextLiked = !currLiked;
    applyOptimistic(nextLiked);
    desiredLikedRef.current = nextLiked;
    scheduleFlush();
  }, [postId, userId]);

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  return { toggle, isBusy: false };
};

/* =========== 인기/검색/내 글/뉴스 =========== */
export const usePopularPostsQuery = (teamId: number) =>
  useQuery<PostListItem[]>({
    queryKey: ['popularPostsAll', teamId],
    enabled: !!teamId,
    staleTime: 60_000,
    queryFn: () => postApi.getPopularByTeam(teamId, 20),
  });

export const useTeamPopularPostsQuery = (teamId: number, limit = 5) =>
  useQuery<PostListItem[]>({
    queryKey: ['popularPostsPreview', teamId, limit],
    enabled: !!teamId,
    staleTime: 60_000,
    queryFn: () => postApi.getPopularByTeam(teamId, limit),
  });

export const useTeamPostSearchInfinite = (teamId: number, keyword: string) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['teamSearch', teamId, keyword],
    enabled: !!teamId && !!keyword?.trim(),
    queryFn: ({ pageParam = undefined }) =>
      postApi.getTeamSearchPosts(teamId, keyword.trim(), pageParam as number | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => (last?.hasNext ? last.nextCursor : undefined),
    staleTime: 60_000,
  });

export const useTeamSearchPostsInfinite = (teamId: number, q: string) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['searchPosts', teamId, (q ?? '').trim()],
    enabled: (teamId ?? 0) > 0 && !!(q ?? '').trim(),
    queryFn: ({ pageParam }) =>
      postApi.searchTeamPosts(teamId, (q ?? '').trim(), pageParam as number | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => (last?.hasNext ? last.nextCursor : undefined),
    staleTime: 60_000,
  });

export const useMyPostsInfinite = (userId?: number) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['myPosts', userId],
    enabled: !!userId,
    queryFn: ({ pageParam = undefined }) =>
      postApi.getMyPosts(userId as number, pageParam as number | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => (last?.hasNext ? last.nextCursor : undefined),
    staleTime: 60_000,
  });

export const useTeamNewsQuery = (teamId?: number, limit = 5) =>
  useQuery<TeamNewsItem[]>({
    queryKey: ['teamNews', teamId, limit],
    enabled: !!teamId,
    staleTime: 5 * 60_000,
    queryFn: () => postApi.getTeamNews(teamId!, limit),
  });

/* ===== 댓글수 전파 ===== */
function patchPostComments(post: any, postId: number, delta: number) {
  if (String(post?.id) !== String(postId)) return post;
  const base =
    typeof post.commentCount === 'number'
      ? post.commentCount
      : typeof post.commentsCount === 'number'
      ? post.commentsCount
      : (post as any).commentCnt ?? 0;
  const next = Math.max(0, base + delta);
  return { ...post, commentCount: next, commentsCount: next, commentCnt: next };
}

export function syncCommentCountEverywhere(
  qc: ReturnType<typeof useQueryClient>,
  postId: number,
  delta: number,
  {
    teamId,
    userId,
    keyword,
  }: { teamId?: number; userId?: number | null; keyword?: string } = {},
) {
  // 상세
  qc.setQueryData(['post', postId], (old: any) =>
    old ? patchPostComments(old, postId, delta) : old,
  );

  // 공통 무한쿼리 패처
  const patchInfinite = (old?: InfiniteData<CursorPostListResponse>) => {
    if (!old) return old;
    return {
      ...(old as any),
      pages: old.pages.map((pg) => ({
        ...(pg as any),
        posts: (pg.posts ?? []).map((p: any) => patchPostComments(p, postId, delta)),
      })),
    } as any;
  };

  // 팀 목록/인기
  if (typeof teamId === 'number') {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['posts', teamId] },
      patchInfinite,
    );
    qc.setQueriesData<any[]>(
      { queryKey: ['popularPostsAll', teamId] },
      (old) =>
        old ? (old as any).map((p: any) => patchPostComments(p, postId, delta)) : old,
    );
    qc.setQueriesData<any[]>(
      { queryKey: ['popularPostsPreview', teamId] },
      (old) =>
        old ? (old as any).map((p: any) => patchPostComments(p, postId, delta)) : old,
    );
  } else {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['posts'] },
      patchInfinite,
    );
  }

  // 내 글
  qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
    typeof userId === 'number' ? { queryKey: ['myPosts', userId] } : { queryKey: ['myPosts'] },
    patchInfinite,
  );

  // 검색
  if (typeof teamId === 'number' && typeof keyword === 'string') {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['searchPosts', teamId, keyword] },
      patchInfinite,
    );
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['teamSearch', teamId, keyword] },
      patchInfinite,
    );
  }
}
