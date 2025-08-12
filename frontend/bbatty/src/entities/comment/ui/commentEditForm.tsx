// entities/comment/ui/commentEditForm.tsx
import React, { useState } from 'react';
import { View, TextInput, Button, StyleSheet } from 'react-native';
import { useUpdateComment } from '../queries/useCommentQueries';
import { useCommentStore } from '../model/store';

interface Props {
  postId: number;
  commentId: number;         // ← number
  initialContent: string;
}

export const CommentEditForm: React.FC<Props> = ({ postId, commentId, initialContent }) => {
  const [content, setContent] = useState(initialContent);
  const updateComment = useUpdateComment(postId);
  const { setEditingCommentId } = useCommentStore();

  const handleSave = () => {
    const trimmed = content.trim();
    if (!trimmed || trimmed.length > 200) return; // 간단 검증
    updateComment.mutate(
      { commentId, content: trimmed },
      { onSuccess: () => setEditingCommentId(null) }
    );
  };

  return (
    <View style={s.wrap}>
      <TextInput
        style={s.input}
        value={content}
        onChangeText={setContent}
        multiline
      />
      <View style={s.row}>
        <Button title="저장" onPress={handleSave} disabled={updateComment.isPending} />
        <View style={{ width: 8 }} />
        <Button title="취소" color="#888" onPress={() => setEditingCommentId(null)} />
      </View>
    </View>
  );
};

const s = StyleSheet.create({
  wrap: { marginTop: 8 },
  input: { borderWidth: 1, borderColor: '#ccc', borderRadius: 6, padding: 10, minHeight: 60, textAlignVertical: 'top' },
  row: { flexDirection: 'row', marginTop: 8 },
});
