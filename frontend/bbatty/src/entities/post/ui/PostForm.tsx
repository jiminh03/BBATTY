// post/ui/PostForm.tsx
import React, { useState } from 'react'
import { useCreatePost } from '../queries/usePostQueries'
import { isValidPost, validatePostContent } from '../utils/vaildation'

export const PostForm: React.FC = () => {
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [error, setError] = useState('')
  const createPost = useCreatePost()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    const validationMessage = validatePostContent(title, content)
    if (!isValidPost({ title, content })) {
      setError(validationMessage ?? '')
      return
    }

    createPost.mutate(
      { title, content, teamId:1 },
      {
        onSuccess: () => {
          setTitle('')
          setContent('')
          setError('')
        },
        onError: () => {
          setError('게시글 생성 실패')
        },
      }
    )
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="제목"
        required
      />
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="내용"
        required
      />
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <button type="submit">게시글 생성</button>
    </form>
  )
}
