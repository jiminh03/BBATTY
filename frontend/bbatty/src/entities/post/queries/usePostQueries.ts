// entities/post/queries/usePostQueries.ts
import { useMutation, useInfiniteQuery, useQueryClient, useQuery } from '@tanstack/react-query';
import { postApi } from '../api/api';
import { CreatePostPayload, CursorPostListResponse } from '../api/types';
import { Post } from '../model/types';
import { apiClient } from '../../../shared/api/client/apiClient';
import { useCallback, useRef, useState } from 'react';

export const usePostListQuery = (teamId: number) =>
  useInfiniteQuery<CursorPostListResponse>({
    queryKey: ['posts', teamId],
    queryFn: ({ pageParam = undefined }) => postApi.getPosts(teamId, pageParam as number | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => (last?.hasNext ? last.nextCursor : undefined),
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

export const usePostDetailQuery = (postId: number) =>
  useQuery<Post>({ queryKey: ['post', postId], queryFn: () => postApi.getPostById(postId) });

// ✅ 게시글 삭제
export const useDeletePostMutation = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (postId: number) => postApi.deletePost(String(postId)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['posts'] }); // 리스트 갱신
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

/* -------------------- 여기부터 추가: 좋아요 -------------------- */
export const usePostLikeActions = (postId: number, options?: { cooldownMs?: number }) => {
  const qc = useQueryClient();
  const detailKey = ['post', postId] as const;
  const cooldownMs = options?.cooldownMs ?? 800;

  // 네트워크 진행 중인지(동시호출 차단)
  const inFlightRef = useRef(false);
  // 연타 중 최종 의도(true=liked, false=unliked)
  const desiredRef = useRef<boolean | null>(null);
  // 버튼 쿨다운 (UI 비활성화)
  const [isCooling, setIsCooling] = useState(false);

  const like = useMutation({
    mutationFn: () => apiClient.post(`/api/posts/${postId}/like`),
    onMutate: async () => {
      await qc.cancelQueries({ queryKey: detailKey });
      const prev = qc.getQueryData<any>(detailKey);
      if (prev) {
        qc.setQueryData(detailKey, {
          ...prev,
          likes: (prev.likes ?? 0) + 1,
          likedByMe: true,
        });
      }
      return { prev, teamId: prev?.teamId };
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(detailKey, ctx.prev);
    },
    onSuccess: (_r, _v, ctx) => {
      if (ctx?.teamId) qc.invalidateQueries({ queryKey: ['posts', ctx.teamId] });
    },
    onSettled: () => {
      inFlightRef.current = false;
      // 요청 끝났고, 최종 의도가 남아있고, 현재 상태와 다르면 딱 한 번 더 전송
      const desired = desiredRef.current;
      desiredRef.current = null;
      const likedNow = (qc.getQueryData<any>(detailKey) as any)?.likedByMe === true;
      if (desired !== null && desired !== likedNow) {
        send(desired ? 'like' : 'unlike');
      }
    },
  });

  const unlike = useMutation({
    mutationFn: () => apiClient.delete(`/api/posts/${postId}/like`),
    onMutate: async () => {
      await qc.cancelQueries({ queryKey: detailKey });
      const prev = qc.getQueryData<any>(detailKey);
      if (prev) {
        qc.setQueryData(detailKey, {
          ...prev,
          likes: Math.max(0, (prev.likes ?? 0) - 1),
          likedByMe: false,
        });
      }
      return { prev, teamId: prev?.teamId };
    },
    onError: (_e, _v, ctx) => {
      if (ctx?.prev) qc.setQueryData(detailKey, ctx.prev);
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

  // 실제 네트워크 전송 (한 번에 하나만)
  function send(intent: 'like' | 'unlike') {
    if (inFlightRef.current) return; // 안전장치
    inFlightRef.current = true;
    if (intent === 'like') like.mutate();
    else unlike.mutate();
  }

  // 화면에서 호출할 토글
  const toggle = useCallback(() => {
    // 쿨다운 중이면 최종 의도만 저장하고 return
    if (isCooling || inFlightRef.current) {
      const likedNow = (qc.getQueryData<any>(detailKey) as any)?.likedByMe === true;
      desiredRef.current = !likedNow;
      return;
    }

    // 쿨다운 시작
    setIsCooling(true);
    const t = setTimeout(() => setIsCooling(false), cooldownMs);
    // 메모리 누수 방지(핫리로드 대비)
    // @ts-ignore
    if (t && t.unref) t.unref?.();

    const likedNow = (qc.getQueryData<any>(detailKey) as any)?.likedByMe === true;
    const intent: 'like' | 'unlike' = likedNow ? 'unlike' : 'like';
    desiredRef.current = !likedNow; // 최종 의도 저장
    send(intent);
  }, [qc, detailKey.join(':'), isCooling, cooldownMs]);

  const isBusy = like.isPending || unlike.isPending || isCooling;

  return { like, unlike, toggle, isBusy };
};