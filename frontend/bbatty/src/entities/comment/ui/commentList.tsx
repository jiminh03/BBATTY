// comment/ui/CommentList.tsx
import React from 'react'
import { View, Text, FlatList, Pressable, ActivityIndicator } from 'react-native'
import { useComments, useDeleteComment } from '../queries/useCommentQueries'
import { useCommentStore } from '../model/store'
import { CommentEditForm } from './commentEditForm'
import { CommentForm } from './CommentForm'
import { Comment } from '../model/types'

interface CommentListProps {
  postId: string
}

export const CommentList: React.FC<CommentListProps> = ({ postId }) => {
  const { data, isLoading, isError } = useComments({ postId, page: 0, size: 10 })
  const deleteComment = useDeleteComment()
  const { editingCommentId, setEditingCommentId } = useCommentStore()

  if (isLoading) return <ActivityIndicator size="large" />
  if (isError || !data) return <Text>댓글을 불러오는 데 실패했습니다.</Text>

  const renderItem = ({ item }: { item: Comment }) => {
    return (
      <View style={{ marginBottom: 16 }}>
        <Text style={{ fontWeight: 'bold' }}>{item.authorNickname}</Text>
        <Text style={{ color: 'gray', fontSize: 12 }}>
          {new Date(item.createdAt).toLocaleString()}
        </Text>

        {editingCommentId === item.id ? (
          <CommentEditForm
            commentId={item.id}
            initialContent={item.content}
          />
        ) : (
          <View style={{ marginTop: 4 }}>
            <Text>{item.isDeleted ? '(삭제된 댓글입니다)' : item.content}</Text>
            {item.isMine && !item.isDeleted && (
              <View style={{ flexDirection: 'row', marginTop: 4 }}>
                <Pressable onPress={() => setEditingCommentId(item.id)} style={{ marginRight: 12 }}>
                  <Text style={{ color: 'blue' }}>수정</Text>
                </Pressable>
                <Pressable onPress={() => deleteComment.mutate({ commentId: item.id })}>
                  <Text style={{ color: 'red' }}>삭제</Text>
                </Pressable>
              </View>
            )}
          </View>
        )}
      </View>
    )
  }

  return (
    <View style={{ padding: 16 }}>
      <CommentForm postId={postId} />

      <FlatList
        data={data.comments}
        renderItem={renderItem}
        keyExtractor={(item) => item.id}
      />
    </View>
  )
}
