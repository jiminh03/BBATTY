// entities/comment/ui/CommentForm.tsx
import React, { useState } from 'react';
import { View, TextInput, Button, StyleSheet, Text, ViewStyle, TouchableOpacity } from 'react-native';
import type { StyleProp } from 'react-native';
import { useCreateComment, useCreateReply } from '../queries/useCommentQueries';
import { useCommentStore } from '../model/store';
// import { useTeamStore } from '@/entities/team/model/store'; // 팀 색상 가져오는 경우

export const FORM_MIN_HEIGHT = 84;

type Props = { 
  postId: number; 
  style?: StyleProp<ViewStyle>; 
  teamColor?: string; // 팀 색상 prop
};

export const CommentForm: React.FC<Props> = ({ postId, style, teamColor = '#FF6F00' }) => {
  const [content, setContent] = useState('');
  const [err, setErr] = useState('');
  const create = useCreateComment(postId);
  const { replyTarget, clearReplyTarget } = useCommentStore();
  const createReply = useCreateReply(postId, replyTarget?.id ?? 0);

  const submit = () => {
    const msg = content.trim();
    if (!msg) return setErr('내용을 입력해주세요.');
    setErr('');

    const onSuccess = () => { 
      setContent(''); 
      clearReplyTarget(); 
    };

    if (replyTarget?.id) {
      createReply.mutate(msg, { onSuccess, onError: () => setErr('답글 생성 실패') });
    } else {
      create.mutate(msg, { onSuccess, onError: () => setErr('댓글 생성 실패') });
    }
  };

  return (
    <View style={[s.wrap, style]}>
      {replyTarget && (
        <View style={s.badge}>
          <Text style={[s.badgeText, { color: teamColor }]}>
            @{replyTarget.nickname ?? '작성자'} ·
          </Text>
          <Text 
            style={[s.badgeText, { textDecorationLine: 'underline', color: teamColor }]} 
            onPress={clearReplyTarget}
          >
            취소
          </Text>
        </View>
      )}
      <TextInput
        style={s.input}
        placeholder={replyTarget ? '답글을 입력하세요' : '댓글을 입력하세요'}
        value={content}
        onChangeText={setContent}
        multiline
      />
      {!!err && <Text style={s.error}>{err}</Text>}
      
      <TouchableOpacity 
        style={[s.button, { backgroundColor: teamColor }]} 
        onPress={submit} 
        disabled={create.isPending || createReply.isPending}
      >
        <Text style={s.buttonText}>
          {(create.isPending || createReply.isPending) 
            ? '작성 중...' 
            : (replyTarget ? '답글 등록' : '댓글 등록')}
        </Text>
      </TouchableOpacity>
    </View>
  );
};

const s = StyleSheet.create({
  wrap: { 
    padding: 12, 
    backgroundColor: '#fff', 
    borderTopWidth: 1, 
    borderColor: '#eee', 
    minHeight: FORM_MIN_HEIGHT 
  },
  badge: { flexDirection: 'row', marginBottom: 6 },
  badgeText: { marginRight: 8, fontWeight: '500' },
  input: { 
    borderWidth: 1, 
    borderColor: '#ccc', 
    borderRadius: 6, 
    padding: 10, 
    minHeight: 60, 
    marginBottom: 8, 
    textAlignVertical: 'top' 
  },
  error: { color: 'red', marginBottom: 6 },
  button: { 
    paddingVertical: 10, 
    borderRadius: 6, 
    alignItems: 'center' 
  },
  buttonText: { 
    color: '#fff', 
    fontWeight: 'bold', 
    fontSize: 16 
  }
});
