// entities/post/queries/usePostQueries.ts
import { useMutation, useInfiniteQuery, useQueryClient, useQuery, InfiniteData } from '@tanstack/react-query';
import { useCallback, useRef, useState, useEffect } from 'react';
import { postApi, TeamNewsItem } from '../api/api';
import { CreatePostPayload, CursorPostListResponse, PostListItem } from '../api/types';
import { Post } from '../model/types';
import { useLikeStore } from '../model/store';
import { useUserStore } from '../../user/model/userStore';

const idNum = (x: any) => (typeof x?.id === 'number' ? x.id : Number(x?.id ?? 0) || 0);

/* ======================== 목록 ======================== */
export const usePostListQuery = (teamId: number) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['posts', teamId],
    queryFn: ({ pageParam }) =>
      postApi.getPosts(teamId, typeof pageParam === 'string' ? Number(pageParam) : (pageParam as number | undefined)),
    initialPageParam: undefined,
    getNextPageParam: (last) =>
      !last?.hasNext
        ? undefined
        : last.nextCursor ?? (last.posts?.length ? idNum(last.posts[last.posts.length - 1]) : undefined),
  });

export const useCreatePost = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreatePostPayload) => postApi.createPost(payload),
    onSuccess: (_r, vars) => qc.invalidateQueries({ queryKey: ['posts', (vars as any).teamId] }),
  });
};

/* ================= 상세(서버우선 + 스토어 fallback) ================= */
export const usePostDetailQuery = (postId: number | null, opts?: { refetchOnFocus?: boolean }) => {
  const userId =
    useUserStore((s: any) => s.currentUser?.id ?? s.currentUser?.userId ?? null) ?? null;

  // 렌더 중 set 금지: userId 바뀔 때만 세션 동기화
  useEffect(() => {
    useLikeStore.getState().setSessionUser(userId);
  }, [userId]);

  const getLiked = useLikeStore((s) => s.getLiked);

  return useQuery<Post>({
    queryKey: ['post', postId],
    queryFn: () => postApi.getPostById(postId!),
    enabled: postId !== null,
    select: (p) => {
      // 좋아요 로컬/서버 합산
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
        typeof local === 'boolean' ? local : typeof serverLikedRaw === 'boolean' ? serverLikedRaw : false;

      // 작성자 닉네임 안전 폴백(탈퇴 사용자 대응)
      const safeAuthor =
        p.authorNickname ??
        (p as any).nickname ??
        '탈퇴한 사용자';

      return {
        ...p,
        authorNickname: safeAuthor,
        isLiked,
        likes: p.likes ?? (p as any).likeCount ?? 0,
      } as Post;
    },
    refetchOnWindowFocus: opts?.refetchOnFocus ?? true,
    staleTime: 3000,
    // v5에서 keepPreviousData 대체
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
function patchPost(post: any, postId: number, liked?: boolean, likeDelta?: number) {
  if (String(post?.id) !== String(postId)) return post;

  const base =
    typeof post.likes === 'number' ? post.likes :
    typeof post.likeCount === 'number' ? post.likeCount : 0;

  const nextLikes = typeof likeDelta === 'number' ? Math.max(0, base + likeDelta) : base;

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
  }
) {
  // 1) 상세
  qc.setQueryData(['post', postId], (old: any) => (old ? patchPost(old, postId, liked, likeDelta) : old));

  // 공통 무한쿼리 패처
  const patchInfinite = (old?: InfiniteData<CursorPostListResponse>) => {
    if (!old) return old;
    return {
      ...(old as any),
      pages: old.pages.map((pg) => ({
        ...(pg as any),
        posts: (pg.posts ?? []).map((p: any) => patchPost(p, postId, liked, likeDelta)),
      })),
    } as any;
  };

  // 2) 팀 목록/인기
  if (typeof teamId === 'number') {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>({ queryKey: ['posts', teamId] }, patchInfinite);
    qc.setQueriesData<any[]>({ queryKey: ['popularPostsAll', teamId] }, (old) =>
      old ? (old as any).map((p: any) => patchPost(p, postId, liked, likeDelta)) : old
    );
    qc.setQueriesData<any[]>({ queryKey: ['popularPostsPreview', teamId] }, (old) =>
      old ? (old as any).map((p: any) => patchPost(p, postId, liked, likeDelta)) : old
    );
  } else {
    // teamId 없으면 prefix 전체를 스캔
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>({ queryKey: ['posts'] }, patchInfinite);
    qc.setQueriesData<any[]>({ queryKey: ['popularPostsAll'] }, (old) =>
      old ? (old as any).map((p: any) => patchPost(p, postId, liked, likeDelta)) : old
    );
    qc.setQueriesData<any[]>({ queryKey: ['popularPostsPreview'] }, (old) =>
      old ? (old as any).map((p: any) => patchPost(p, postId, liked, likeDelta)) : old
    );
  }

  // 3) 내 글
  qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
    typeof userId === 'number' ? { queryKey: ['myPosts', userId] } : { queryKey: ['myPosts'] },
    patchInfinite
  );

  // 4) 검색
  if (typeof teamId === 'number' && typeof keyword === 'string') {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['searchPosts', teamId, keyword] },
      patchInfinite
    );
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['teamSearch', teamId, keyword] },
      patchInfinite
    );
  } else {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>({ queryKey: ['searchPosts'] }, patchInfinite);
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>({ queryKey: ['teamSearch'] }, patchInfinite);
  }
}

/* =================== 좋아요 토글 =================== */
export const usePostLikeActions = (
  postId: number,
  options?: {
    teamId?: number;
    listKeyword?: string;
    onRequireLogin?: () => void;
    /** 사용자 연타를 병합하는 대기 시간(ms). */
    debounceMs?: number; // 기본 500
    /** 서버 확인용 재검증 지연(ms). 0이면 즉시 재검증하지 않음. */
    refetchAfterMs?: number; // 기본 300
  }
) => {
  const qc = useQueryClient();
  const userId =
    useUserStore((s: any) => s.currentUser?.id ?? s.currentUser?.userId ?? null) ?? null;

  const teamId = options?.teamId;
  const keyword = options?.listKeyword;
  const DEBOUNCE_MS = options?.debounceMs ?? 500;
  const REFETCH_AFTER = options?.refetchAfterMs ?? 300;

  // 세션 유저 전달(렌더 중 set 금지 → effect)
  useEffect(() => {
    useLikeStore.getState().setSessionUser(userId);
  }, [userId]);

  const setLikedStore = useLikeStore((s) => s.setLiked);
  const getLikedStore = useLikeStore((s) => s.getLiked);

  const detailKey = ['post', postId] as const;

  /** 마지막으로 서버에 동기화된 값 */
  const serverLikedRef = useRef<boolean | null>(null);
  /** 사용자가 현재 UI상 원한다고 보는 목표값(연타로 계속 바뀜) */
  const desiredLikedRef = useRef<boolean | null>(null);
  /** 서버 요청 진행 중 여부 */
  const inFlightRef = useRef(false);
  /** 디바운스 타이머 */
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 최초 서버 값 주입
  useEffect(() => {
    const curr = qc.getQueryData<Post>(detailKey) as Post | undefined;
    const liked =
      typeof curr?.isLiked === 'boolean'
        ? curr.isLiked
        : typeof (curr as any)?.likedByMe === 'boolean'
        ? (curr as any).likedByMe
        : !!(curr as any)?.liked;
    serverLikedRef.current = !!liked;
    if (desiredLikedRef.current === null) desiredLikedRef.current = !!liked;
  }, [qc, postId]);

  /** 공통 낙관 반영 */
  const applyOptimistic = (liked: boolean) => {
    setLikedStore(postId, liked, userId);
    qc.setQueryData<Post>(detailKey, (old) => {
      if (!old) return old as any;
      return {
        ...(old as any),
        isLiked: liked,
        likedByMe: liked,
        liked,
      } as Post;
    });
    // 리스트에도 liked 플래그만 반영(카운트는 서버 결과로만)
    syncLikeEverywhere(qc, postId, { liked, likeDelta: undefined, teamId, userId, keyword });
  };

  /** 현재 캐시 liked/likes 읽기 */
  const readCache = () => {
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
    return { liked: !!liked, likes };
  };

  /** 서버로 실제 동기화(막타만 전송, 요청 중이면 대기) */
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
      })
      .catch(() => {
        // 실패 시 서버 값 모름 → 안전하게 재검증
        qc.invalidateQueries({ queryKey: detailKey });
        serverLikedRef.current = null; // 다음 flush 때 재결정
      })
      .finally(() => {
        inFlightRef.current = false;

        // 사용자가 여전히 서버와 다른 상태를 원하면, 다시 "조용해질 때" 보내도록 디바운스 재시작
        qc.invalidateQueries({ queryKey: detailKey });
        if (typeof teamId === 'number') {
          qc.invalidateQueries({ queryKey: ['posts', teamId] });
          qc.invalidateQueries({ queryKey: ['popularPostsAll', teamId] });
          qc.invalidateQueries({ queryKey: ['popularPostsPreview', teamId] });
        }
      });
  }, [postId, qc, DEBOUNCE_MS, REFETCH_AFTER]);

  /** 디바운스 스케줄(트레일링만) */
  const scheduleFlush = () => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => {
      timerRef.current = null;
      flush();
    }, DEBOUNCE_MS);
  };

  /** 사용자 탭: 즉시 낙관 반영 + 목표값만 업데이트 */
  const toggle = useCallback(() => {
    if (!userId) {
      options?.onRequireLogin?.();
      return;
    }

    const storeLiked = getLikedStore(postId, userId);
    const cacheLiked = readCache().liked;
    const currLiked = (typeof storeLiked === 'boolean' ? storeLiked : cacheLiked) ?? false;

    const nextLiked = !currLiked;

    // 1) 즉시 UI 업데이트
    applyOptimistic(nextLiked);

    // 2) 목표값 업데이트
    desiredLikedRef.current = nextLiked;

    // 3) 서버는 조용해진 뒤에 1회만
    scheduleFlush();
  }, [postId, userId]);

  // 메모리 정리
  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  // 연타 가능: isBusy는 항상 false
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
    queryFn: ({ pageParam }) => postApi.searchTeamPosts(teamId, (q ?? '').trim(), pageParam as number | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => (last?.hasNext ? last.nextCursor : undefined),
    staleTime: 60_000,
  });

export const useMyPostsInfinite = (userId?: number) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['myPosts', userId],
    enabled: !!userId,
    queryFn: ({ pageParam = undefined }) => postApi.getMyPosts(userId as number, pageParam as number | undefined),
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

  return {
    ...post,
    commentCount: next,
    commentsCount: next,
    commentCnt: next,
  };
}

export function syncCommentCountEverywhere(
  qc: ReturnType<typeof useQueryClient>,
  postId: number,
  delta: number,
  {
    teamId,
    userId,
    keyword,
  }: { teamId?: number; userId?: number | null; keyword?: string } = {}
) {
  // 상세
  qc.setQueryData(['post', postId], (old: any) =>
    old ? patchPostComments(old, postId, delta) : old
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
      patchInfinite
    );
    qc.setQueriesData<any[]>(
      { queryKey: ['popularPostsAll', teamId] },
      (old) => (old ? (old as any).map((p: any) => patchPostComments(p, postId, delta)) : old)
    );
    qc.setQueriesData<any[]>(
      { queryKey: ['popularPostsPreview', teamId] },
      (old) => (old ? (old as any).map((p: any) => patchPostComments(p, postId, delta)) : old)
    );
  } else {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['posts'] },
      patchInfinite
    );
  }

  // 내 글
  qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
    typeof userId === 'number' ? { queryKey: ['myPosts', userId] } : { queryKey: ['myPosts'] },
    patchInfinite
  );

  // 검색
  if (typeof teamId === 'number' && typeof keyword === 'string') {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['searchPosts', teamId, keyword] },
      patchInfinite
    );
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['teamSearch', teamId, keyword] },
      patchInfinite
    );
  }
}
