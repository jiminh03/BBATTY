// entities/post/queries/usePostQueries.ts
import { useMutation, useInfiniteQuery, useQueryClient, useQuery } from '@tanstack/react-query';
import { useCallback, useRef, useState } from 'react';
import { postApi } from '../api/api';
import { CreatePostPayload, CursorPostListResponse, PostListItem } from '../api/types';
import { Post } from '../model/types';
import { apiClient } from '../../../shared/api/client/apiClient';
import { useLikeStore } from '../model/store';

/* -------------------- 리스트/작성 -------------------- */
export const usePostListQuery = (teamId: number) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['posts', teamId],
    queryFn: ({ pageParam }) => {
      const cursor =
        typeof pageParam === 'string' ? Number(pageParam) : (pageParam as number | undefined);
      return postApi.getPosts(teamId, cursor);
    },
    initialPageParam: undefined,
    getNextPageParam: (last) => {
      if (!last?.hasNext) return undefined;
      const fallback = last.posts?.length ? last.posts[last.posts.length - 1].id : undefined;
      return (last.nextCursor ?? fallback) as number | undefined;
    },
  });


export const useCreatePost = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreatePostPayload) => postApi.createPost(payload),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['posts', vars.teamId] });
    },
  });
};

/* -------------------- 상세(로컬 liked 병합) -------------------- */
export const usePostDetailQuery = (postId: number) => {
  const likedLocal = useLikeStore((s) => s.byPostId[postId]);
  const countLocal = useLikeStore((s) => s.byPostCount[postId]);
  const tsLocal = useLikeStore((s) => s.ts[postId]);
  const TTL = 24 * 60 * 60 * 1000; // 24시간 (원하면 조정/제거 가능)
  const isFresh = !!tsLocal && Date.now() - tsLocal < TTL;

  return useQuery<Post>({
    queryKey: ['post', postId],
    queryFn: () => postApi.getPostById(postId),
    select: (p) => ({
      ...p,
      // 서버가 값을 주면 서버 우선, 없으면 로컬 보완
      likedByMe: (p as any)?.likedByMe ?? likedLocal ?? false,
      // 숫자는 TTL 안의 로컬 값이 있으면 그걸 우선, 아니면 서버
      likes: isFresh && countLocal !== undefined ? countLocal : (p.likes ?? 0),
    }),
  });
};

/* -------------------- 삭제/수정 -------------------- */
export const useDeletePostMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (postId: number) => postApi.deletePost(String(postId)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['posts'] });
    },
  });
};

export const useUpdatePost = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { postId: number; title: string; content: string; teamId?: number; status?: string; postStatus?: string }) =>
      postApi.updatePost(vars.postId, vars),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: ['post', vars.postId] });
      if (vars.teamId) qc.invalidateQueries({ queryKey: ['posts', vars.teamId] });
    },
  });
};

/* -------------------- 좋아요 토글(스팸 방지 + 스토어 동기화) -------------------- */
export const usePostLikeActions = (postId: number, options?: { cooldownMs?: number }) => {
  const qc = useQueryClient();
  const setLikedStore = useLikeStore((s) => s.setLiked);
  const setCountStore = useLikeStore((s) => s.setCount);
  const detailKey = ['post', postId] as const;
  const cooldownMsRef = useRef(options?.cooldownMs ?? 800);
  const cooldownTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inFlightRef = useRef(false);             // 네트워크 진행중
  const desiredRef = useRef<boolean | null>(null); // 연타 동안 최종 의도
  const coolingRef = useRef(false);              // 동기 게이트(즉시 차단)
  const [isCooling, setIsCooling] = useState(false);

   const getBaselineLikes = () => {
    const local = useLikeStore.getState().byPostCount[postId];
    const q = (qc.getQueryData<any>(detailKey) as any)?.likes;
    return (local ?? q ?? 0) as number;
  };

  const like = useMutation({
    mutationFn: () => apiClient.post(`/api/posts/${postId}/like`),
    onMutate: async () => {
      await qc.cancelQueries({ queryKey: detailKey });
      const prev = qc.getQueryData<any>(detailKey);
      const base = getBaselineLikes();
      const next = base + 1;

      if (prev) {
        qc.setQueryData(detailKey, { ...prev, likes: next, likedByMe: true });
      }
      setLikedStore(postId, true);
      setCountStore(postId, next);                 // 👈 숫자도 스토어에 저장

      return { prev, prevCount: prev?.likes, teamId: prev?.teamId };
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(detailKey, ctx.prev);
      setLikedStore(postId, !!(ctx?.prev as any)?.likedByMe);
      setCountStore(postId, ctx?.prevCount);      // 👈 롤백
    },
    onSuccess: (_r, _v, ctx) => {
      if (ctx?.teamId) qc.invalidateQueries({ queryKey: ['posts', ctx.teamId] });
      // 상세 invalidate 안 함 (깜빡임 방지)
    },
    onSettled: () => {
      // ... 기존 coalesce 유지 ...
    },
  });

  const unlike = useMutation({
    mutationFn: () => apiClient.delete(`/api/posts/${postId}/like`),
    onMutate: async () => {
      await qc.cancelQueries({ queryKey: detailKey });
      const prev = qc.getQueryData<any>(detailKey);
      const base = getBaselineLikes();
      const next = Math.max(0, base - 1);

      if (prev) {
        qc.setQueryData(detailKey, { ...prev, likes: next, likedByMe: false });
      }
      setLikedStore(postId, false);
      setCountStore(postId, next);                 // 👈 숫자도 스토어에 저장

      return { prev, prevCount: prev?.likes, teamId: prev?.teamId };
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(detailKey, ctx.prev);
      setLikedStore(postId, !!(ctx?.prev as any)?.likedByMe);
      setCountStore(postId, ctx?.prevCount);      // 👈 롤백
    },
    onSuccess: (_r, _v, ctx) => {
      if (ctx?.teamId) qc.invalidateQueries({ queryKey: ['posts', ctx.teamId] });
    },
    onSettled: () => {
      inFlightRef.current = false;
      const desired = desiredRef.current;
      desiredRef.current = null;
      const likedNow = (qc.getQueryData<any>(detailKey) as any)?.likedByMe === true;
      if (desired !== null && desired !== likedNow) {
        send(desired ? 'like' : 'unlike');
      }
    },
  });

  function startCooldown() {
    coolingRef.current = true;      // 동기적으로 즉시 차단
    setIsCooling(true);
    setTimeout(() => {
      coolingRef.current = false;
      setIsCooling(false);
    }, cooldownMsRef.current);
  }

  function send(intent: 'like' | 'unlike') {
    if (inFlightRef.current) return;
    inFlightRef.current = true;
    if (intent === 'like') like.mutate();
    else unlike.mutate();
  }

  const toggle = useCallback(() => {
    const likedNow = (qc.getQueryData<any>(detailKey) as any)?.likedByMe === true;

    if (coolingRef.current || inFlightRef.current) {
      desiredRef.current = !likedNow; // 최종 의도만 저장
      return;
    }

    startCooldown();
    desiredRef.current = !likedNow;
    send(likedNow ? 'unlike' : 'like');
  }, [qc, detailKey.join(':'), cooldownMsRef.current]);

  const isBusy = like.isPending || unlike.isPending || isCooling;

  return { like, unlike, toggle, isBusy };
};

// entities/post/queries/usePostQueries.ts
export const usePopularPostsQuery = (teamId: number) =>
  useQuery<PostListItem[]>({
    queryKey: ['popularPostsAll', teamId],          // (키 분리하면 캐시 혼동 방지)
    enabled: !!teamId,
    staleTime: 60_000,
    queryFn: () => postApi.getPopularByTeam(teamId, 20), // ✅ 최대 20개
  });

export const useTeamPopularPostsQuery = (teamId: number, limit = 5) =>
  useQuery<PostListItem[]>({
    queryKey: ['popularPostsPreview', teamId, limit],
    enabled: !!teamId,
    staleTime: 60_000,
    queryFn: () => postApi.getPopularByTeam(teamId, limit), // 홈 미리보기 5개
  });

/** 팀별 게시글 검색(무한 스크롤) */
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
    enabled: !!userId,                           // userId 있어야 동작
    queryFn: ({ pageParam = undefined }) =>
      postApi.getMyPosts(userId as number, pageParam as number | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => (last?.hasNext ? last.nextCursor : undefined),
    staleTime: 60_000,
  });