// comment/model/store.ts
import { create } from 'zustand'

interface CommentStore {
  editingCommentId: string | null
  setEditingCommentId: (id: string | null) => void
}

export const useCommentStore = create<CommentStore>((set) => ({
  editingCommentId: null,
  setEditingCommentId: (id) => set({ editingCommentId: id }),
}))
