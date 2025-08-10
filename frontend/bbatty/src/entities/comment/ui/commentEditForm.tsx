// entities/comment/ui/commentEditForm.tsx
import React, { useState } from 'react';
import { View, TextInput, Button } from 'react-native';
import { useUpdateComment } from '../queries/useCommentQueries';
import { useCommentStore } from '../model/store';

interface Props {
  commentId: string;
  initialContent: string;
}

export const CommentEditForm: React.FC<Props> = ({ commentId, initialContent }) => {
  const [content, setContent] = useState(initialContent);
  const updateComment = useUpdateComment(/* postId 필요하면 prop으로 전달 */ 0);
  const { setEditingCommentId } = useCommentStore();

  const handleUpdate = () => {
    if (!content.trim()) {
      // 토스트 써도 됨
      return;
    }
    updateComment.mutate(
      { commentId, content },
      {
        onSuccess: () => setEditingCommentId(null),
      }
    );
  };

  return (
    <View style={{ marginTop: 8 }}>
      <TextInput
        multiline
        value={content}
        onChangeText={setContent}
        style={{ borderWidth: 1, borderColor: '#ccc', borderRadius: 6, padding: 10, minHeight: 60 }}
      />
      <View style={{ marginTop: 8 }}>
        <Button title="저장" onPress={handleUpdate} />
      </View>
    </View>
  );
};
