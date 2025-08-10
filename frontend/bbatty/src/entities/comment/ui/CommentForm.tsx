// entities/comment/ui/CommentForm.tsx
import React, { useState } from 'react';
import { View, TextInput, Button, StyleSheet, Text } from 'react-native';
import { useCreateComment } from '../queries/useCommentQueries';

export const CommentForm = ({ postId }: { postId: number }) => {
  const [content, setContent] = useState('');
  const [err, setErr] = useState('');
  const create = useCreateComment(postId); // ✅ 반드시 존재해야 함

  const submit = () => {
    const msg = content.trim();
    if (!msg) return setErr('내용을 입력해주세요.');
    create.mutate(msg, {
      onSuccess: () => { setContent(''); setErr(''); },
      onError: () => setErr('댓글 생성 실패'),
    });
  };

  return (
    <View style={s.wrap}>
      <TextInput
        style={s.input}
        placeholder="댓글을 입력하세요"
        value={content}
        onChangeText={setContent}
        multiline
      />
      {!!err && <Text style={s.error}>{err}</Text>}
      <Button title={create.isPending ? '작성 중...' : '댓글 등록'} onPress={submit} disabled={create.isPending}/>
    </View>
  );
};

const s = StyleSheet.create({
  wrap:{ padding:12, backgroundColor:'#fff', borderTopWidth:1, borderColor:'#eee' },
  input:{ borderWidth:1, borderColor:'#ccc', borderRadius:6, padding:10, minHeight:60, marginBottom:8, textAlignVertical:'top' },
  error:{ color:'red', marginBottom:6 },
});
