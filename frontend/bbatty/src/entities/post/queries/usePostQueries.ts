// entities/post/queries/usePostQueries.ts
import {
  useMutation,
  useInfiniteQuery,
  useQueryClient,
  useQuery,
  InfiniteData,
} from '@tanstack/react-query';
import { useCallback, useRef, useState, useEffect } from 'react';
import { postApi, TeamNewsItem } from '../api/api';
import { CreatePostPayload, CursorPostListResponse, PostListItem } from '../api/types';
import { Post } from '../model/types';
import { useLikeStore } from '../model/store';
import { useUserStore } from '../../user/model/userStore';

const idNum = (x: any) => (typeof x?.id === 'number' ? x.id : Number(x?.id ?? 0) || 0);

// ---------- 목록 ----------
export const usePostListQuery = (teamId: number) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['posts', teamId],
    queryFn: ({ pageParam }) =>
      postApi.getPosts(
        teamId,
        typeof pageParam === 'string' ? Number(pageParam) : (pageParam as number | undefined),
      ),
    initialPageParam: undefined,
    getNextPageParam: (last) =>
      !last?.hasNext
        ? undefined
        : (last.nextCursor ??
            (last.posts?.length ? idNum(last.posts[last.posts.length - 1]) : undefined)),
  });

export const useCreatePost = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreatePostPayload) => postApi.createPost(payload),
    onSuccess: (_r, vars) => qc.invalidateQueries({ queryKey: ['posts', (vars as any).teamId] }),
  });
};

// ---------- 상세(서버우선 + 유저별 liked fallback) ----------
export const usePostDetailQuery = (
  postId: number,
  opts?: { refetchOnFocus?: boolean }
) => {
  const userId =
    useUserStore((s: any) => s.currentUser?.id ?? s.currentUser?.userId ?? null) ?? null;

  // 렌더 중 set 금지: userId 바뀔 때만 세션 동기화
  useEffect(() => {
    useLikeStore.getState().setSessionUser(userId);
  }, [userId]);

  const getLiked = useLikeStore((s) => s.getLiked);

  return useQuery<Post>({
    queryKey: ['post', postId],
    queryFn: () => postApi.getPostById(postId),
    select: (p) => {
      // 1) 스토어(유저별) 최우선
      const local = getLiked(postId, userId);

      // 2) 서버가 확실한 boolean을 주면 그것도 고려
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

      // 카운트는 서버 기준(낙관 업데이트는 캐시에서만)
      return { ...p, isLiked, likes: p.likes ?? 0 } as Post;
    },
    refetchOnWindowFocus: opts?.refetchOnFocus ?? true,
    staleTime: 3000,
    placeholderData: (prev) => prev,
  });
};

// ---------- 삭제/수정 ----------
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

// ---------- 목록 전파 유틸 ----------
function patchPost(post: Post, postId: number, liked?: boolean, likeDelta?: number): Post {
  if (idNum(post) !== postId) return post;
  const nextLiked = typeof liked === 'boolean' ? liked : post.isLiked;
  const base = typeof post.likes === 'number' ? post.likes : 0;
  const nextLikes = typeof likeDelta === 'number' ? Math.max(0, base + likeDelta) : base;
  return { ...post, isLiked: nextLiked, likes: nextLikes };
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
  }: { liked?: boolean; likeDelta?: number; teamId?: number; userId?: number | null; keyword?: string },
) {
  // 1) 상세
  qc.setQueryData<Post>(['post', postId], (old) =>
    old ? patchPost(old, postId, liked, likeDelta) : old,
  );

  // 2) 팀 목록
  if (typeof teamId === 'number') {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['posts', teamId] },
      (old) => {
        if (!old) return old;
        return {
          ...(old as any),
          pages: old.pages.map((pg) => ({
            ...(pg as any),
            posts: (pg.posts ?? []).map((p: any) =>
              patchPost(p as Post, postId, liked, likeDelta),
            ) as any,
          })),
        } as any;
      },
    );
    qc.setQueriesData<PostListItem[]>(
      { queryKey: ['popularPostsAll', teamId] },
      (old) =>
        old ? (old as any).map((p: any) => patchPost(p as Post, postId, liked, likeDelta)) : old,
    );
    qc.setQueriesData<PostListItem[]>(
      { queryKey: ['popularPostsPreview', teamId] },
      (old) =>
        old ? (old as any).map((p: any) => patchPost(p as Post, postId, liked, likeDelta)) : old,
    );
  }

  // 3) 내 글
  if (typeof userId === 'number') {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['myPosts', userId] },
      (old) => {
        if (!old) return old;
        return {
          ...(old as any),
          pages: old.pages.map((pg) => ({
            ...(pg as any),
            posts: (pg.posts ?? []).map((p: any) =>
              patchPost(p as Post, postId, liked, likeDelta),
            ) as any,
          })),
        } as any;
      },
    );
  }

  // 4) 검색
  if (typeof teamId === 'number' && typeof keyword === 'string') {
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['searchPosts', teamId, keyword] },
      (old) => {
        if (!old) return old;
        return {
          ...(old as any),
          pages: old.pages.map((pg) => ({
            ...(pg as any),
            posts: (pg.posts ?? []).map((p: any) =>
              patchPost(p as Post, postId, liked, likeDelta),
            ) as any,
          })),
        } as any;
      },
    );
    qc.setQueriesData<InfiniteData<CursorPostListResponse>>(
      { queryKey: ['teamSearch', teamId, keyword] },
      (old) => {
        if (!old) return old;
        return {
          ...(old as any),
          pages: old.pages.map((pg) => ({
            ...(pg as any),
            posts: (pg.posts ?? []).map((p: any) =>
              patchPost(p as Post, postId, liked, likeDelta),
            ) as any,
          })),
        } as any;
      },
    );
  }
}

// ---------- 좋아요 토글 ----------
export const usePostLikeActions = (postId: number, options?: {
  teamId?: number;
  listKeyword?: string;
  cooldownMs?: number;
  refetchAfterMs?: number;
  onRequireLogin?: () => void;
}) => {
  const qc = useQueryClient();
  const userId =
    useUserStore((s: any) => s.currentUser?.id ?? s.currentUser?.userId ?? null) ?? null;
  const teamId = options?.teamId;
  const keyword = options?.listKeyword;
  const cooldownMs = options?.cooldownMs ?? 350;
  const refetchAfterMs = options?.refetchAfterMs ?? 0;

  // ✅ 렌더 중 set 금지
  useEffect(() => {
    useLikeStore.getState().setSessionUser(userId);
  }, [userId]);

  const setLikedStore = useLikeStore((s) => s.setLiked);
  const getLikedStore = useLikeStore((s) => s.getLiked);

  const lastAppliedRef = useRef<null | 'like' | 'unlike'>(null);

  const detailKey = ['post', postId] as const;

  const coolingRef = useRef(false);
  const inFlightRef = useRef<null | 'like' | 'unlike'>(null);
  const lastIntentRef = useRef<null | 'like' | 'unlike'>(null);
  const [isCooling, setIsCooling] = useState(false);

  const startCooldown = () => {
    coolingRef.current = true;
    setIsCooling(true);
    setTimeout(() => {
      coolingRef.current = false;
      setIsCooling(false);
    }, cooldownMs);
  };

  const getSnapshot = () => {
    const prev = qc.getQueryData<Post>(detailKey);
    const likedNow =
      typeof prev?.isLiked === 'boolean'
        ? prev!.isLiked
        : getLikedStore(postId, userId) ?? false;
    const countNow = typeof prev?.likes === 'number' ? prev!.likes : 0;
    return { prev, likedNow, countNow, teamId: (prev as any)?.teamId };
  };

  const like = useMutation({
    mutationFn: () => postApi.likePost(postId),
    onMutate: async () => {
      if (!userId) { options?.onRequireLogin?.(); throw new Error('UNAUTHENTICATED'); }
      await qc.cancelQueries({ queryKey: detailKey });
      const prev = qc.getQueryData<Post>(detailKey);
      const base = typeof prev?.likes === 'number' ? prev!.likes : 0;
      const next = base + 1;

      // ✅ 먼저 스토어 갱신(의사결정의 SOT)
      setLikedStore(postId, true, userId);

      // ✅ 캐시(상세 + 전파) 반영
      qc.setQueryData<Post>(detailKey, (old) => (old ? { ...old, isLiked: true, likes: next } : old));
      syncLikeEverywhere(qc, postId, { liked: true, likeDelta: +1, teamId, userId, keyword });

      lastAppliedRef.current = 'like';
      return { prev, base };
    },
    onError: (_e, _v, ctx) => {
      if (!ctx) return;
      // 정확 롤백
      setLikedStore(postId, (ctx.prev?.isLiked ?? false), userId);
      qc.setQueryData<Post>(detailKey, (old) => (old ? { ...old, isLiked: ctx.prev?.isLiked ?? false, likes: ctx.base } : ctx.prev));
      syncLikeEverywhere(qc, postId, { liked: ctx.prev?.isLiked, likeDelta: -1, teamId, userId, keyword });
      lastAppliedRef.current = null;
    },
    onSettled: () => { /* 그대로 */ inFlightRef.current = null; if (refetchAfterMs>0) setTimeout(()=>qc.invalidateQueries({queryKey:detailKey}), refetchAfterMs); if (lastIntentRef.current){const p=lastIntentRef.current; lastIntentRef.current=null; send(p);} }
  });

  const unlike = useMutation({
    mutationFn: () => postApi.unlikePost(postId),
    onMutate: async () => {
      if (!userId) { options?.onRequireLogin?.(); throw new Error('UNAUTHENTICATED'); }
      await qc.cancelQueries({ queryKey: detailKey });
      const prev = qc.getQueryData<Post>(detailKey);
      const base = typeof prev?.likes === 'number' ? prev!.likes : 0;
      const next = Math.max(0, base - 1);

      // ✅ 먼저 스토어 갱신
      setLikedStore(postId, false, userId);

      // ✅ 캐시 반영
      qc.setQueryData<Post>(detailKey, (old) => (old ? { ...old, isLiked: false, likes: next } : old));
      syncLikeEverywhere(qc, postId, { liked: false, likeDelta: -1, teamId, userId, keyword });

      lastAppliedRef.current = 'unlike';
      return { prev, base };
    },
    onError: (_e, _v, ctx) => {
      if (!ctx) return;
      setLikedStore(postId, (ctx.prev?.isLiked ?? false), userId);
      qc.setQueryData<Post>(detailKey, (old) => (old ? { ...old, isLiked: ctx.prev?.isLiked ?? false, likes: ctx.base } : ctx.prev));
      syncLikeEverywhere(qc, postId, { liked: ctx.prev?.isLiked, likeDelta: +1, teamId, userId, keyword });
      lastAppliedRef.current = null;
    },
    onSettled: () => { /* 그대로 */ inFlightRef.current = null; if (refetchAfterMs>0) setTimeout(()=>qc.invalidateQueries({queryKey:detailKey}), refetchAfterMs); if (lastIntentRef.current){const p=lastIntentRef.current; lastIntentRef.current=null; send(p);} }
  });

  function send(intent: 'like'|'unlike') {
    // 같은 의도를 연속으로 보내려 하면 무시(보호막)
    if (inFlightRef.current || lastAppliedRef.current === intent) {
      lastIntentRef.current = intent;
      return;
    }
    inFlightRef.current = intent;
    intent === 'like' ? like.mutate() : unlike.mutate();
  }

   const toggle = useCallback(() => {
    // ✅ 판정은 “스토어 우선 → 캐시 보조”
    const likedFromStore = getLikedStore(postId, userId);
    const likedFromCache = qc.getQueryData<Post>(detailKey)?.isLiked;
    const likedNow = (typeof likedFromStore === 'boolean' ? likedFromStore : likedFromCache) ?? false;

    if (!userId) { options?.onRequireLogin?.(); return; }
    if (coolingRef.current || inFlightRef.current) {
      lastIntentRef.current = likedNow ? 'unlike' : 'like';
      return;
    }
    startCooldown();
    send(likedNow ? 'unlike' : 'like');
  }, [postId, userId]);

  const isBusy =
    like.isPending || unlike.isPending || isCooling || inFlightRef.current !== null;
  return { toggle, isBusy: like.isPending || unlike.isPending || isCooling || inFlightRef.current !== null };
};

// ---------- 인기/검색/내 글/뉴스 ----------
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
