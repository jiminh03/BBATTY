// entities/comment/ui/CommentForm.tsx
import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  TextInput,
  StyleSheet,
  Text,
  ViewStyle,
  TouchableOpacity,
  Keyboard,
  Platform,
  Animated,
} from 'react-native';
import type { StyleProp } from 'react-native';
import { useCreateComment, useCreateReply } from '../queries/useCommentQueries';
import { useCommentStore } from '../model/store';

export const FORM_MIN_HEIGHT = 56; // ↓ 낮춤

type Props = {
  postId: number;
  style?: StyleProp<ViewStyle>;
  teamColor?: string; // 팀 색상 prop
};

export const CommentForm: React.FC<Props> = ({
  postId,
  style,
  teamColor = '#000000ff',
}) => {
  const [content, setContent] = useState('');
  const [err, setErr] = useState('');
  const [inputHeight, setInputHeight] = useState(40); // ↓ 기본 더 낮게

  const create = useCreateComment(postId);
  const { replyTarget, clearReplyTarget } = useCommentStore();
  const createReply = useCreateReply(postId, replyTarget?.id ?? 0);

  // 안드로이드: 키보드 뜨면 폼도 같이 위로
  const translateY = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    if (Platform.OS !== 'android') return;
    const showSub = Keyboard.addListener('keyboardDidShow', (e) => {
      const h = e.endCoordinates?.height ?? 0;
      Animated.timing(translateY, {
        toValue: -h,
        duration: 180,
        useNativeDriver: true,
      }).start();
    });
    const hideSub = Keyboard.addListener('keyboardDidHide', () => {
      Animated.timing(translateY, {
        toValue: 0,
        duration: 160,
        useNativeDriver: true,
      }).start();
    });
    return () => {
      showSub.remove();
      hideSub.remove();
    };
  }, [translateY]);

  const isSubmitting = create.isPending || createReply.isPending;

  const submit = () => {
    const msg = content.trim();
    if (!msg) return setErr('내용을 입력해주세요.');
    setErr('');

    const onSuccess = () => {
      setContent('');
      setInputHeight(40);
      clearReplyTarget();
      Keyboard.dismiss();
    };

    if (replyTarget?.id) {
      createReply.mutate(msg, {
        onSuccess,
        onError: () => setErr('답글 생성 실패'),
      });
    } else {
      create.mutate(msg, {
        onSuccess,
        onError: () => setErr('댓글 생성 실패'),
      });
    }
  };

  return (
    <Animated.View style={[s.wrap, style, { transform: [{ translateY }] }]}>
      {replyTarget && (
        <View style={s.badgeRow}>
          <Text style={[s.badgeText, { color: teamColor }]}>
            @{replyTarget.nickname ?? '작성자'} ·
          </Text>
          <Text
            style={[s.badgeText, s.badgeAction, { color: teamColor }]}
            onPress={clearReplyTarget}
          >
            취소
          </Text>
        </View>
      )}

      <View style={s.row}>
        <TextInput
          style={[
            s.input,
            {
              height: Math.min(120, Math.max(36, inputHeight)), // 36~120 사이로 자동
            },
          ]}
          placeholder={replyTarget ? '답글을 입력하세요' : '댓글을 입력하세요'}
          value={content}
          onChangeText={setContent}
          onContentSizeChange={(e) =>
            setInputHeight(e.nativeEvent.contentSize.height)
          }
          multiline
          textAlignVertical="top"
          returnKeyType="send"
          onSubmitEditing={submit}
          blurOnSubmit={false}
        />

        <TouchableOpacity
          style={[s.button, { backgroundColor: teamColor }]}
          onPress={submit}
          disabled={isSubmitting}
          activeOpacity={0.85}
        >
          <Text style={s.buttonText}>
            {isSubmitting ? '작성 중…' : replyTarget ? '답글' : '등록'}
          </Text>
        </TouchableOpacity>
      </View>

      {!!err && <Text style={s.error}>{err}</Text>}
    </Animated.View>
  );
};

const s = StyleSheet.create({
  wrap: {
    paddingHorizontal: 10,
    paddingTop: 8,
    paddingBottom: 8,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderColor: '#eee',
    minHeight: FORM_MIN_HEIGHT,
  },
  badgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
  },
  badgeText: { marginRight: 8, fontWeight: '600', fontSize: 12.5 },
  badgeAction: { textDecorationLine: 'underline' },
  row: { flexDirection: 'row', alignItems: 'flex-end', gap: 8 },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#DADCE0',
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingTop: 8,
    paddingBottom: 8,
    fontSize: 14,
    minHeight: 36,
    maxHeight: 120,
    backgroundColor: '#FAFAFA',
  },
  button: {
    height: 40,
    paddingHorizontal: 14,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: 72,
  },
  buttonText: { color: '#fff', fontWeight: '700', fontSize: 14 },
  error: { color: '#FF3B30', marginTop: 6, fontSize: 12.5 },
});
