// entities/post/api/api.ts
import axios from 'axios';
import { apiClient, extractData } from '../../../shared/api';
import { CreatePostPayload, UpdatePostPayload, PostListItem, CursorPostListResponse } from './types';
import { Post } from '../model/types';

export type TeamNewsItem = {
  id?: number;
  postId?: number;
  title: string;
  summary?: string;
  thumbnailUrl?: string;
  publishedAt?: string;
  source?: string;
  link?: string;
};

/** 에러 객체(코드/HTTP 포함) */
export class ApiError extends Error {
  code?: string;
  httpStatus?: number;
  constructor(message: string, code?: string, httpStatus?: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.httpStatus = httpStatus;
  }
}

const toNum = (v: unknown): number =>
  typeof v === 'number' ? v : Number(v ?? 0) || 0;

/** 서버 응답을 우리 Post 타입에 맞게 정규화 + 안전 폴백 */
function normalizePost(raw: any): Post {
  const likes = toNum(raw?.likes ?? raw?.likeCount ?? raw?.likesCount);
  const views = toNum(raw?.views ?? raw?.viewCount);
  const commentCount = toNum(raw?.commentCount ?? raw?.commentsCount);

  // teamId 통일
  const teamId = toNum(
    raw?.teamId ??
      raw?.team_id ??
      raw?.teamID ??
      raw?.team?.id ??
      raw?.team?.teamId ??
      raw?.team?.team_id
  );

  // 작성자 닉네임 폴백(탈퇴 사용자 대응)
  const authorNicknameRaw =
    raw?.authorNickname ??
    raw?.nickname ??
    raw?.author?.nickname ??
    raw?.writer?.nickname ??
    null;

  // 서버가 작성자 삭제/탈퇴를 명시할 수 있는 흔한 키들
  const authorDeleted =
    raw?.authorDeleted === true ||
    raw?.isAuthorDeleted === true ||
    raw?.author?.status === 'DELETED' ||
    raw?.writer?.status === 'DELETED';

  const authorNickname =
    authorNicknameRaw ??
    (authorDeleted ? '탈퇴한 사용자' : '탈퇴한 사용자'); // 닉네임이 비어있으면 동일 폴백

  const isLiked =
    raw?.isLiked === true ||
    raw?.liked === true ||
    raw?.likedByMe === true ||
    String(raw?.isLiked ?? raw?.liked ?? raw?.likedByMe) === 'true';

  return {
    ...(raw as object),
    teamId: teamId || undefined,
    likes,
    views,
    commentCount,
    isLiked,
    authorNickname,
  } as Post;
}

/** 공통 응답 파서: status/data/message 패턴 지원 + 에러코드 전파 */
function unwrap<T = any>(res: any): T {
  // axios 응답
  const root = (res as any)?.data ?? res;
  const status = root?.status;
  if (status && status !== 'SUCCESS') {
    const code = root?.code ?? root?.errorCode ?? root?.error?.code;
    const msg = root?.message ?? root?.error?.message ?? '요청 실패';
    throw new ApiError(msg, code, (res as any)?.status ?? undefined);
  }
  // status 래핑 없을 때 data 사용
  return (status ? root?.data : root) as T;
}

/** axios 에러 → ApiError 로 변환 */
function asApiError(e: unknown, fallbackMsg: string) {
  if (axios.isAxiosError(e)) {
    const http = e.response?.status;
    const data = e.response?.data as any;
    const code = data?.code ?? data?.errorCode ?? data?.error?.code;
    const msg =
      data?.message ??
      data?.error?.message ??
      e.message ??
      fallbackMsg;
    return new ApiError(msg, code, http);
  }
  if (e instanceof ApiError) return e;
  return new ApiError((e as any)?.message ?? fallbackMsg);
}

export const postApi = {
  async getTeamNews(teamId: number, limit = 5): Promise<TeamNewsItem[]> {
    const res = await apiClient.get(`/api/posts/team/${teamId}/news`, { params: { limit } });
    const payload: any = unwrap(res);

    const list: any[] = Array.isArray(payload)
      ? payload
      : Array.isArray(payload?.items)
      ? payload.items
      : Array.isArray(payload?.news)
      ? payload.news
      : [];

    return list.slice(0, limit).map((n: any) => ({
      id: n?.id ?? n?.newsId ?? n?.postId,
      postId: n?.postId,
      title: String(n?.title ?? '제목 없음'),
      summary: n?.summary ?? n?.description ?? '',
      thumbnailUrl: n?.thumbnailUrl ?? n?.imageUrl ?? n?.thumbnail ?? undefined,
      publishedAt: n?.publishedAt ?? n?.createdAt ?? undefined,
      source: n?.source ?? n?.publisher ?? undefined,
      link: n?.link ?? n?.url ?? undefined,
    }));
  },

  createPost: (payload: CreatePostPayload) => apiClient.post('/api/posts', payload),

  updatePost: async (postId: number, payload: UpdatePostPayload & { teamId?: number }) => {
    try {
      const body = {
        title: payload.title,
        content: payload.content,
        ...(payload.teamId != null ? { teamId: payload.teamId } : {}),
      };
      const res = await apiClient.put<any>(`/api/posts/${postId}`, body);
      const apiRes: any = (res as any)?.data ?? res;
      return 'data' in apiRes ? extractData<any>(apiRes) : apiRes;
    } catch (e) {
      throw asApiError(e, '게시글 수정 실패');
    }
  },

  deletePost: (postId: number) => apiClient.delete(`/api/posts/${postId}`),

  async getPostById(postId: number): Promise<Post> {
    try {
      const res = await apiClient.get(`/api/posts/${postId}`);
      const raw = unwrap<any>(res);
      if (!raw) throw new ApiError('게시글 상세 조회 실패', 'POST_NOT_FOUND', (res as any)?.status);
      return normalizePost(raw);
    } catch (e) {
      throw asApiError(e, '게시글 상세 조회 실패');
    }
  },

  async getPosts(teamId: number, cursor?: number): Promise<CursorPostListResponse> {
    try {
      const params: Record<string, any> = {};
      if (cursor !== undefined) params.cursor = cursor;

      const res = await apiClient.get(`/api/posts/team/${teamId}`, { params });
      const raw = unwrap<any>(res);
      if (!raw) throw new ApiError('게시글 목록 조회 실패', 'LIST_FAIL', (res as any)?.status);

      const posts = Array.isArray(raw?.posts) ? raw.posts.map(normalizePost) : [];
      const hasNext = Boolean(raw?.hasNext);
      const nextRaw = raw?.nextCursor;
      const nextCursor =
        typeof nextRaw === 'number'
          ? nextRaw
          : typeof nextRaw === 'string' && nextRaw !== ''
          ? Number(nextRaw)
          : undefined;

      return { posts, hasNext, nextCursor };
    } catch (e) {
      throw asApiError(e, '게시글 목록 조회 실패');
    }
  },

  async getPopularByTeam(teamId: number, limit = 20) {
    let cursor: number | undefined;
    const acc: PostListItem[] = [];
    const seen = new Set<string>();

    while (acc.length < limit) {
      const res = await apiClient.get(`/api/posts/team/${teamId}/popular`, {
        params: cursor !== undefined ? { cursor } : {},
      });
      const api = (res as any).data as any;
      const pageRaw: any[] = Array.isArray(api?.data) ? api.data : api?.data?.posts ?? [];
      const page = pageRaw.map(normalizePost) as unknown as PostListItem[];

      for (const p of page) {
        const key = String((p as any).id);
        if (!seen.has(key)) {
          acc.push(p);
          seen.add(key);
          if (acc.length >= limit) break;
        }
      }
      const hasNext = !Array.isArray(api?.data) && api?.data?.hasNext === true;
      cursor = !Array.isArray(api?.data) ? api?.data?.nextCursor : undefined;
      if (!hasNext) break;
    }
    return acc.slice(0, limit);
  },

  // api.ts
  async likePost(postId: number) {
    const res = await apiClient.post(`/api/posts/${postId}/like`);
    return res.data; // ✅ 여기서 data만 반환
  },

  unlikePost(postId: number) {
    return apiClient.delete(`/api/posts/${postId}/like`);
  },

  deletePostImage: (postId: number, imageUrl: string) =>
    apiClient.delete(`/api/posts/${postId}/images`, { params: { imageUrl } } as any),

  async getTeamSearchPosts(teamId: number, keyword: string, cursor?: number): Promise<CursorPostListResponse> {
    try {
      const res = await apiClient.get(`/api/posts/team/${teamId}/search`, {
        params: { keyword, ...(cursor !== undefined ? { cursor } : {}) },
      });
      const data = unwrap<any>(res);
      if (!data) throw new ApiError('검색 실패', 'SEARCH_FAIL', (res as any)?.status);

      const posts = Array.isArray(data?.posts) ? data.posts.map(normalizePost) : [];
      const hasNext = Boolean(data?.hasNext);
      const nextCursor =
        typeof data?.nextCursor === 'number'
          ? data.nextCursor
          : typeof data?.nextCursor === 'string' && data.nextCursor !== ''
          ? Number(data.nextCursor)
          : undefined;

      return { posts, hasNext, nextCursor };
    } catch (e) {
      throw asApiError(e, '검색 실패');
    }
  },

  async searchTeamPosts(teamId: number, rawQuery: string, cursor?: number) {
    const query = (rawQuery ?? '').trim();
    if (!teamId || teamId <= 0 || !query) return { posts: [], hasNext: false, nextCursor: undefined };

    const params: Record<string, any> = { keyword: query, q: query };
    if (cursor !== undefined) params.cursor = cursor;

    try {
      const res = await apiClient.get(`/api/posts/team/${teamId}/search`, { params });
      const data = unwrap<any>(res);
      if (!data) return { posts: [], hasNext: false, nextCursor: undefined };

      const posts = Array.isArray(data?.posts) ? data.posts.map(normalizePost) : [];
      const hasNext = Boolean(data?.hasNext);
      const nextRaw = data?.nextCursor;
      const nextCursor =
        typeof nextRaw === 'number'
          ? nextRaw
          : typeof nextRaw === 'string' && nextRaw !== ''
          ? Number(nextRaw)
          : undefined;

      return { posts, hasNext, nextCursor };
    } catch (e) {
      throw asApiError(e, '검색 실패');
    }
  },

  async getMyPosts(userId: number, cursor?: number): Promise<CursorPostListResponse> {
    const params: Record<string, any> = {};
    if (cursor !== undefined) params.cursor = cursor;

    const res = await apiClient.get(`/api/posts/user/${userId}`, { params });
    const root: any = (res as any)?.data ?? res;
    const payload = root?.data ?? root;

    const postsRaw = Array.isArray(payload?.posts)
      ? payload.posts
      : Array.isArray(payload?.content)
      ? payload.content
      : Array.isArray(payload)
      ? payload
      : [];
    const posts = postsRaw.map(normalizePost);

    const hasNext = Boolean(payload?.hasNext ?? payload?.page?.hasNext ?? false);
    const rawNext =
      payload?.nextCursor ?? payload?.page?.nextCursor ?? payload?.next ?? payload?.nextId ?? payload?.next_id;
    const nextCursor =
      typeof rawNext === 'string' ? Number(rawNext) : typeof rawNext === 'number' ? rawNext : undefined;

    return { posts, hasNext, nextCursor };
  },
};
