// entities/post/ui/PostItem.tsx
import React, { memo, useMemo } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useNavigation } from '@react-navigation/native';

// 최소 속성만 사용 (리스트 아이템은 쿼리에서 normalize 되어 옴)
type AnyPost = {
  id: number | string;
  title?: string;
  nickname?: string;
  createdAt?: string;
  // 정규화 필드
  likes?: number;
  likeCount?: number;
  isLiked?: boolean;
  likedByMe?: boolean;
  liked?: boolean;
  commentCount?: number;
  commentsCount?: number;
  viewCount?: number;
  views?: number;
  teamId?: number;
};

type Props = {
  post: AnyPost;
  teamId?: number; // 목록 화면에서 전달 (없으면 post.teamId 사용)
  onPress?: () => void;
};

function PostItemBase({ post, teamId, onPress }: Props) {
  const navigation = useNavigation<any>();

  const handlePress = () => {
    if (onPress) onPress();
    else {
      const teamIdParam =
        typeof post.teamId === 'number' ? post.teamId : (typeof teamId === 'number' ? teamId : undefined);
      navigation.navigate('PostDetail', { postId: Number(post.id), teamId: teamIdParam });
    }
  };

  // ✅ 표시는 항상 아이템의 정규화 필드를 신뢰 (스토어/다른 캐시 직접 접근 금지)
  const liked =
    typeof post.isLiked === 'boolean'
      ? post.isLiked
      : typeof post.likedByMe === 'boolean'
      ? post.likedByMe
      : !!post.liked;

  const likeCount =
    typeof post.likes === 'number'
      ? post.likes
      : typeof post.likeCount === 'number'
      ? post.likeCount
      : 0;

  const commentCount =
    typeof post.commentCount === 'number'
      ? post.commentCount
      : typeof post.commentsCount === 'number'
      ? post.commentsCount
      : 0;

  const views =
    typeof post.views === 'number'
      ? post.views
      : typeof post.viewCount === 'number'
      ? post.viewCount
      : 0;

  const timeText = useMemo(() => {
    try {
      const d = new Date(post.createdAt ?? 0);
      const hh = String(d.getHours()).padStart(2, '0');
      const mm = String(d.getMinutes()).padStart(2, '0');
      return `${hh}:${mm}`;
    } catch {
      return '';
    }
  }, [post.createdAt]);

  return (
    <TouchableOpacity onPress={handlePress} activeOpacity={0.8}>
      <View style={s.card}>
        <View style={s.content}>
          <Text style={s.title} numberOfLines={1}>
            {post.title ?? `#${post.id}`}
          </Text>

          <View style={s.metaRow}>
            {!!post.nickname && <Text style={s.meta}>{post.nickname}</Text>}
            {!!post.nickname && <Text style={s.dot}>·</Text>}
            <Text style={s.meta}>{timeText}</Text>
            <Text style={s.dot}>·</Text>
            <Text style={s.meta}>👁 {views}</Text>
            <Text style={s.dot}>·</Text>
            <Text style={s.meta}>{liked ? '🤍' : '🤍'} {likeCount}</Text>
            <Text style={s.dot}>·</Text>
            <Text style={s.meta}>💬 {commentCount}</Text>
          </View>
        </View>
      </View>
    </TouchableOpacity>
  );
}

// likes/isLiked/댓글/조회수 변화도 감지하도록 비교
export const PostItem = memo(
  PostItemBase,
  (prev, next) =>
    String(prev.post.id) === String(next.post.id) &&
    (prev.post.likes ?? prev.post.likeCount) === (next.post.likes ?? next.post.likeCount) &&
    (prev.post.isLiked ?? prev.post.likedByMe ?? prev.post.liked) ===
      (next.post.isLiked ?? next.post.likedByMe ?? next.post.liked) &&
    (prev.post.viewCount ?? prev.post.views) === (next.post.viewCount ?? next.post.views) &&
    (prev.post.commentCount ?? prev.post.commentsCount) ===
      (next.post.commentCount ?? next.post.commentsCount) &&
    prev.teamId === next.teamId
);

const s = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingVertical: 12,
    paddingHorizontal: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderColor: '#eee',
  },
  content: { flex: 1, paddingRight: 12 },
  title: { fontWeight: '800', fontSize: 16, color: '#111', marginBottom: 4 },
  metaRow: { flexDirection: 'row', alignItems: 'center', marginTop: 6, flexWrap: 'wrap' },
  meta: { fontSize: 12, color: '#888' },
  dot: { marginHorizontal: 6, color: '#bbb' },
});

export default PostItem;
