import { create } from 'zustand';
import { Post, PostSortType } from './types';

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
  reset: () =>
    set({
      posts: [],
      selectedPost: null,
      sortType: PostSortType.LATEST,
      isLoading: false,
    }),
}));
