// comment/ui/CommentForm.tsx
import React, { useState } from 'react'
import { View, TextInput, Button, Alert } from 'react-native'
import { useCreateComment } from '../queries/useCommentQueries'
import { isValidComment } from '../utils/validation'

interface CommentFormProps {
  postId: string
  parentId?: string | null
  depth?: number
}

export const CommentForm: React.FC<CommentFormProps> = ({
  postId,
  parentId = null,
  depth = 0,
}) => {
  const [content, setContent] = useState('')
  const createComment = useCreateComment()

  const handleSubmit = () => {
    if (!isValidComment(content)) {
      Alert.alert('오류', '댓글은 1자 이상 200자 이하여야 합니다.')
      return
    }

    createComment.mutate(
      { postId, content, parentId, depth },
      {
        onSuccess: () => {
          setContent('') // 작성 후 초기화
        },
        onError: (error: any) => {
          Alert.alert('댓글 작성 실패', error.message || '잠시 후 다시 시도해주세요.')
        },
      }
    )
  }

  return (
    <View style={{ marginBottom: 16 }}>
      <TextInput
        placeholder="댓글을 입력하세요"
        value={content}
        onChangeText={setContent}
        multiline
        style={{
          borderWidth: 1,
          borderColor: '#ccc',
          padding: 8,
          borderRadius: 6,
          minHeight: 60,
          marginBottom: 8,
        }}
      />
      <Button title="댓글 작성" onPress={handleSubmit} />
    </View>
  )
}
