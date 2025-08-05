import React from 'react'
import { useComments, useDeleteComment } from '../queries/useCommentQueries'
import { Comment } from '../model/types'
import { useCommentStore } from '../model/store'
import { CommentEditForm } from './commentEditForm'

interface CommentListProps {
  postId: string
}

export const CommentList: React.FC<CommentListProps> = ({ postId }) => {
  const { data, isLoading, isError } = useComments({ postId, page: 0, size: 10 })
  const deleteComment = useDeleteComment()
  const { editingCommentId, setEditingCommentId } = useCommentStore() // ✅ 컴포넌트 안에서 사용

  if (isLoading) return <p>댓글 불러오는 중...</p>
  if (isError || !data) return <p>댓글을 불러오는 데 실패했습니다.</p>

  return (
    <ul>
      {data.comments.map((comment: Comment) => (
        <li key={comment.id}>
          <div style={{ marginBottom: '4px' }}>
            <strong>{comment.authorNickname}</strong>
            <span style={{ marginLeft: '8px', color: '#888' }}>
              {new Date(comment.createdAt).toLocaleString()}
            </span>
          </div>

          {editingCommentId === comment.id ? (
            <CommentEditForm
              commentId={comment.id}
              initialContent={comment.content}
            />
          ) : (
            <>
              <div>{comment.content}</div>
              {comment.isMine && (
                <div style={{ marginTop: '4px' }}>
                  <button
                    onClick={() => setEditingCommentId(comment.id)}
                    style={{ marginRight: '8px' }}
                  >
                    수정
                  </button>
                  <button
                    onClick={() => deleteComment.mutate({ commentId: comment.id })}
                    style={{ color: 'red' }}
                  >
                    삭제
                  </button>
                </div>
              )}
            </>
          )}
        </li>
      ))}
    </ul>
  )
}
