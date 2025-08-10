// entities/comment/ui/commentEditForm.tsx
import React, { useState } from 'react';
import { View, TextInput, Button } from 'react-native';
import { useUpdateComment } from '../queries/useCommentQueries';

interface Props {
  postId: number;
  commentId: string;           // ✅ string으로
  initialContent: string;
}

export const CommentEditForm: React.FC<Props> = ({ postId, commentId, initialContent }) => {
  const [content, setContent] = useState(initialContent);
  const update = useUpdateComment(postId);

  const handleUpdate = () => {
    const msg = content.trim();
    if (!msg) return;
    update.mutate({ commentId, content }); // ✅ string 그대로 전달
  };

  return (
    <View style={{ marginTop: 8 }}>
      <TextInput
        style={{ borderWidth: 1, borderColor: '#ccc', borderRadius: 6, padding: 10, minHeight: 60 }}
        value={content}
        onChangeText={setContent}
        multiline
      />
      <View style={{ height: 8 }} />
      <Button title={update.isPending ? '저장 중...' : '저장'} onPress={handleUpdate} disabled={update.isPending} />
    </View>
  );
};
