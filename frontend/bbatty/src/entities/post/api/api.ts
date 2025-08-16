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

const toNum = (v: unknown): number => (typeof v === 'number' ? v : Number(v ?? 0) || 0);
const idNum = (x: any) => (typeof x?.id === 'number' ? x.id : Number(x?.id ?? 0) || 0);

/** 서버 응답을 우리 Post 타입에 맞게 정규화 */
function normalizePost(raw: any): Post {
  const likes = toNum(raw?.likes ?? raw?.likeCount ?? raw?.likesCount);
  const views = toNum(raw?.views ?? raw?.viewCount);
  const commentCount = toNum(raw?.commentCount ?? raw?.commentsCount);
  const isLiked =
    raw?.isLiked === true ||
    raw?.liked === true ||
    raw?.likedByMe === true ||
    String(raw?.isLiked ?? raw?.liked ?? raw?.likedByMe) === 'true';

  return {
    ...(raw as object),
    // id는 string일 수도 있으니 그대로 유지(비교 시 숫자화)
    likes,
    views,
    commentCount,
    isLiked,
  } as Post;
}

export const postApi = {
  async getTeamNews(teamId: number, limit = 5): Promise<TeamNewsItem[]> {
    const res = await apiClient.get(`/api/posts/team/${teamId}/news`, { params: { limit } });
    const root: any = (res as any)?.data ?? res;
    const payload = root?.status === 'SUCCESS' ? root?.data : root?.data ?? root;

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
    const body = {
      title: payload.title,
      content: payload.content,
      ...(payload.teamId != null ? { teamId: payload.teamId } : {}),
    };
    const res = await apiClient.put<any>(`/api/posts/${postId}`, body);
    const apiRes: any = (res as any)?.data ?? res;
    return 'data' in apiRes ? extractData<any>(apiRes) : apiRes;
  },

  deletePost: (postId: number) => apiClient.delete(`/api/posts/${postId}`),

  getPostById: async (postId: number): Promise<Post> => {
    const res = await apiClient.get(`/api/posts/${postId}`);
    const api = (res as any).data as { status?: string; message?: string; data?: any };
    const raw = api?.data ?? (api?.status ? undefined : (res as any).data);
    if (!raw) throw new Error(api?.message ?? '게시글 상세 조회 실패');
    return normalizePost(raw);
  },

  getPosts: async (teamId: number, cursor?: number): Promise<CursorPostListResponse> => {
    const params: Record<string, any> = {};
    if (cursor !== undefined) params.cursor = cursor;

    const res = await apiClient.get(`/api/posts/team/${teamId}`, { params });
    const api = (res as any).data as { status?: string; message?: string; data?: any };
    const raw = api?.data ?? (api?.status ? undefined : (res as any).data);
    if (!raw) throw new Error(api?.message ?? '게시글 목록 조회 실패');

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

  likePost(postId: number) {
    return apiClient.post(`/api/posts/${postId}/like`);
  },
  unlikePost(postId: number) {
    return apiClient.delete(`/api/posts/${postId}/like`);
  },

  deletePostImage: (postId: number, imageUrl: string) =>
    apiClient.delete(`/api/posts/${postId}/images`, { params: { imageUrl } }),

  async getTeamSearchPosts(teamId: number, keyword: string, cursor?: number): Promise<CursorPostListResponse> {
    const res = await apiClient.get(`/api/posts/team/${teamId}/search`, {
      params: { keyword, ...(cursor !== undefined ? { cursor } : {}) },
    });
    const api = (res as any).data as { status?: string; message?: string; data?: any };
    const data = api?.data;
    if (!data) throw new Error(api?.message ?? '검색 실패');

    const posts = Array.isArray(data?.posts) ? data.posts.map(normalizePost) : [];
    const hasNext = Boolean(data?.hasNext);
    const nextCursor =
      typeof data?.nextCursor === 'number'
        ? data.nextCursor
        : typeof data?.nextCursor === 'string' && data.nextCursor !== ''
        ? Number(data.nextCursor)
        : undefined;

    return { posts, hasNext, nextCursor };
  },

  async searchTeamPosts(teamId: number, rawQuery: string, cursor?: number) {
    const query = (rawQuery ?? '').trim();
    if (!teamId || teamId <= 0 || !query) return { posts: [], hasNext: false, nextCursor: undefined };

    const params: Record<string, any> = { keyword: query, q: query };
    if (cursor !== undefined) params.cursor = cursor;

    const res = await apiClient.get(`/api/posts/team/${teamId}/search`, { params });
    const root: any = (res as any)?.data ?? res;
    const data = root?.data;
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
