// comment/model/store.ts
import { create } from 'zustand';

interface ReplyTarget {
  id: number;
  nickname?: string | null;
}

interface CommentStore {
  editingCommentId: string | null;
  setEditingCommentId: (id: string | null) => void;

  replyTarget: ReplyTarget | null;                 // ✅ 추가
  setReplyTarget: (t: ReplyTarget | null) => void; // ✅ 추가
  clearReplyTarget: () => void;                    // ✅ 추가
}

export const useCommentStore = create<CommentStore>((set) => ({
  editingCommentId: null,
  setEditingCommentId: (id) => set({ editingCommentId: id }),

  replyTarget: null,
  setReplyTarget: (t) => set({ replyTarget: t }),
  clearReplyTarget: () => set({ replyTarget: null }),
}));
