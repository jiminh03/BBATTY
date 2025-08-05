// comment/ui/CommentEditForm.tsx
import React, { useState } from 'react'
import { useUpdateComment } from '../queries/useCommentQueries'
import { useCommentStore } from '../model/store'
import { isValidComment } from '../utils/validation'

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

  const handleUpdate = () => {
    if (!isValidComment(content)) {
      alert('댓글은 1자 이상 200자 이하여야 합니다.')
      return
    }

    updateComment.mutate(
      { commentId, content },
      {
        onSuccess: () => {
          setEditingCommentId(null) // 수정 종료
        },
      }
    )
  }

  const handleCancel = () => {
    setEditingCommentId(null)
  }

  return (
    <div style={{ marginTop: '0.5rem' }}>
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={3}
        style={{ width: '100%' }}
      />
      <div style={{ marginTop: '0.5rem' }}>
        <button onClick={handleUpdate} style={{ marginRight: '0.5rem' }}>
          저장
        </button>
        <button onClick={handleCancel} style={{ color: 'gray' }}>
          취소
        </button>
      </div>
    </div>
  )
}
