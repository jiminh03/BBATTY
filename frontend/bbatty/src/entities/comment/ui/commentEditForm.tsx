// comment/ui/CommentEditForm.tsx
import React, { useState } from 'react'
import { useUpdateComment } from '../queries/useCommentQueries'
import { useCommentStore } from '../model/store'

interface CommentEditFormProps {
  commentId: string
  initialContent: string
}

export const CommentEditForm: React.FC<CommentEditFormProps> = ({
  commentId,
  initialContent,
}) => {
  const [content, setContent] = useState(initialContent)
  const updateComment = useUpdateComment()
  const { setEditingCommentId } = useCommentStore()

  const handleUpdate = (e: React.FormEvent) => {
    e.preventDefault()
    updateComment.mutate(
      { commentId, content },
      {
        onSuccess: () => {
          setEditingCommentId(null)
        },
      }
    )
  }

  return (
    <form onSubmit={handleUpdate}>
      <input
        value={content}
        onChange={(e) => setContent(e.target.value)}
        style={{ width: '300px' }}
      />
      <button type="submit" style={{ marginLeft: '8px' }}>
        저장
      </button>
      <button
        type="button"
        onClick={() => setEditingCommentId(null)}
        style={{ marginLeft: '4px' }}
      >
        취소
      </button>
    </form>
  )
}
