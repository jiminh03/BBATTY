// entities/post/model/store.ts
import { create } from 'zustand';
import { createJSONStorage, persist, StateStorage } from 'zustand/middleware';     // v3/v4 공통
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Post, PostSortType } from './types';

/* ---------------------- 포스트 스토어 (그대로) ---------------------- */
interface PostState {
  posts: Post[];
  selectedPost: Post | null;
  sortType: PostSortType;
  isLoading: boolean;
}
interface PostActions {
  setPosts: (posts: Post[]) => void;
  setSelectedPost: (post: Post | null) => void;
  setSortType: (sortType: PostSortType) => void;
  setLoading: (loading: boolean) => void;
  reset: () => void;
}
type PostStore = PostState & PostActions;

export const usePostStore = create<PostStore>((set) => ({
  posts: [],
  selectedPost: null,
  sortType: PostSortType.LATEST,
  isLoading: false,
  setPosts: (posts) => set({ posts }),
  setSelectedPost: (post) => set({ selectedPost: post }),
  setSortType: (sortType) => set({ sortType }),
  setLoading: (loading) => set({ isLoading: loading }),
  reset: () => set({
    posts: [],
    selectedPost: null,
    sortType: PostSortType.LATEST,
    isLoading: false,
  }),
}));

/* ---------------------- 좋아요 스토어 (타입 추가) ---------------------- */

type LikeStore = {
  byPostId: Record<number, boolean | undefined>;
  byPostCount: Record<number, number | undefined>;
  ts: Record<number, number | undefined>;
  setLiked: (postId: number, liked: boolean) => void;
  setCount: (postId: number, count: number | undefined) => void;
  clear: () => void;
};

export const useLikeStore = create<LikeStore>()(
  persist<LikeStore>(
    (set) => ({
      byPostId: {},
      byPostCount: {},
      ts: {},
      setLiked: (postId: number, liked: boolean) =>
        set((s) => ({
          byPostId: { ...s.byPostId, [postId]: liked },
          ts: { ...s.ts, [postId]: Date.now() },
        })),
      setCount: (postId: number, count: number | undefined) =>
        set((s) => ({
          byPostCount: { ...s.byPostCount, [postId]: count },
          ts: { ...s.ts, [postId]: Date.now() },
        })),
      clear: () => set({ byPostId: {}, byPostCount: {}, ts: {} }),
    }),
    {
      name: 'likeStore-v1',
      storage: createJSONStorage(() => AsyncStorage), // ✅ v4
    }
  )
);