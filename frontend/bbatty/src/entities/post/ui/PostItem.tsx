import { useNavigation } from '@react-navigation/native';
import React, { useMemo } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useQueryClient } from '@tanstack/react-query';
import { useLikeStore } from '../model/store';
import type { PostListItem } from '../api/types';

type AnyCmt = {
  id?: number | string;
  commentId?: number | string;
  content?: string;
  authorNickname?: string;
  nickname?: string;
  createdAt?: string;
  updatedAt?: string;
  is_deleted?: number;
  isDeleted?: boolean;
  replies?: AnyCmt[];
};

type PostItemProps = {
  post: PostListItem;
  onPress?: () => void; // ← 추가
};

export const PostItem = ({ post, onPress }: PostItemProps) => {
  const navigation = useNavigation<any>();
  const qc = useQueryClient();

  const handlePress = () => {
    if (onPress) {
      onPress();
    } else {
      navigation.navigate('PostDetail', { postId: post.id });
    }
  };

  // 상세 캐시
  const detail = qc.getQueryData<any>(['post', post.id]) as any | undefined;

  // 좋아요: 로컬 스토어 우선
  const likedLocal = useLikeStore((s) => s.byPostId[post.id]);
  const likeCountLocal = useLikeStore((s) => s.byPostCount[post.id]);

  const liked = (detail?.likedByMe ?? likedLocal) ?? false;
  const likeCount = (likeCountLocal ?? detail?.likes ?? post.likeCount ?? 0) as number;
  const commentCount = (detail?.commentCount ?? post.commentCount ?? 0) as number;
  const views = (detail?.views ?? detail?.viewCount ?? post.viewCount ?? 0) as number;

  // 댓글 캐시 평탄화 → 최신 1~2개만 미리보기
  const cmtCache = qc.getQueryData<any>(['comments', post.id]);
  const parents: AnyCmt[] = (cmtCache?.pages ?? []).flatMap((p: any) => p?.comments ?? []);

  const flat: AnyCmt[] = useMemo(() => {
    const out: AnyCmt[] = [];
    const dfs = (n: AnyCmt) => {
      const id = Number(n.id ?? n.commentId);
      const isDeleted = Number(n?.is_deleted ?? (n as any)?.isDeleted ?? 0) === 1 || !!n?.isDeleted;
      out.push({ ...n, id, isDeleted });
      if (Array.isArray(n.replies)) n.replies.forEach(dfs);
    };
    parents.forEach(dfs);
    return out;
  }, [parents]);

  const preview: AnyCmt[] = useMemo(() => {
    const byTime = (c: AnyCmt) => new Date((c.updatedAt ?? c.createdAt) ?? 0).getTime();
    return flat.slice().sort((a, b) => byTime(a) - byTime(b)).slice(-2);
  }, [flat]);

  const timeText = useMemo(() => {
    try {
      const d = new Date(post.createdAt);
      const hh = String(d.getHours()).padStart(2, '0');
      const mm = String(d.getMinutes()).padStart(2, '0');
      return `${hh}:${mm}`;
    } catch {
      return '';
    }
  }, [post.createdAt]);

  return (
    <TouchableOpacity
      onPress={handlePress} activeOpacity={0.8}>
      <View style={s.card}>
        {/* 왼쪽 본문 */}
        <View style={s.content}>
          <Text style={s.title} numberOfLines={1}>
            {post.title}
          </Text>

          {/* 한 줄 프리뷰(있으면) */}
          {!!(post as any)?.content && (
            <Text style={s.preview} numberOfLines={1}>
              {(post as any).content}
            </Text>
          )}

          <View style={s.metaRow}>
            <Text style={s.meta}>{post.nickname}</Text>
            <Text style={s.dot}>·</Text>
            <Text style={s.meta}>{timeText}</Text>
            <Text style={s.dot}>·</Text>
            <Text style={s.meta}>👁 {views}</Text>
            <Text style={s.dot}>·</Text>
            <Text style={s.meta}>{liked ? '❤️' : '🤍'} {likeCount}</Text>
          </View>

          {/* 댓글 미리보기(있을 때만) */}
          {preview.length > 0 && (
            <View style={s.previewWrap}>
              {preview.map((c) => {
                const nick = c.authorNickname ?? c.nickname ?? '익명';
                const text = c.isDeleted ? '(삭제된 댓글입니다)' : (c.content ?? '');
                return (
                  <Text key={String(c.id)} style={s.previewLine} numberOfLines={1}>
                    <Text style={s.previewNick}>{nick}:</Text> {text}
                  </Text>
                );
              })}
            </View>
          )}
        </View>

        {/* 오른쪽 댓글 배지 */}
        <View style={s.cBadge}>
          <Text style={s.cNum}>{commentCount}</Text>
          <Text style={s.cLabel}>댓글</Text>
        </View>
      </View>
    </TouchableOpacity>
  );
};

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
  preview: { fontSize: 13, color: '#666' },
  metaRow: { flexDirection: 'row', alignItems: 'center', marginTop: 6 },
  meta: { fontSize: 12, color: '#888' },
  dot: { marginHorizontal: 6, color: '#bbb' },

  // 오른쪽 댓글 박스(배지)
  cBadge: {
    width: 56,
    height: 60,
    borderRadius: 12,
    backgroundColor: '#F2F2F2',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 8,
  },
  cNum: { fontWeight: '800', fontSize: 16, color: '#222' },
  cLabel: { fontSize: 12, color: '#888', marginTop: 2 },

  // 댓글 미리보기
  previewWrap: { marginTop: 6 },
  previewLine: { fontSize: 12, color: '#444', marginTop: 2 },
  previewNick: { fontWeight: '600' },
});

export default PostItem;
