// PostItem.tsx
import { useNavigation } from '@react-navigation/native';
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

export const PostItem = ({ post }: { post: PostListItem }) => {
  const navigation = useNavigation<any>();
  const qc = useQueryClient();

  // 상세 캐시
  const detail = qc.getQueryData<any>(['post', post.id]) as any | undefined;

  // 좋아요: 로컬 스토어 우선
  const likedLocal = useLikeStore((s) => s.byPostId[post.id]);
  const likeCountLocal = useLikeStore((s) => s.byPostCount[post.id]);

  const liked = (detail?.likedByMe ?? likedLocal) ?? false;
  const likeCount = (likeCountLocal ?? detail?.likes ?? post.likeCount ?? 0) as number;
  const commentCount = (detail?.commentCount ?? post.commentCount ?? 0) as number;
  const views = (detail?.views ?? detail?.viewCount ?? post.viewCount ?? 0) as number;

  // 댓글 캐시에서 최신 1~2개 미리보기 (네트워크 호출 X)
  const cmtCache = qc.getQueryData<any>(['comments', post.id]);
  const parents: AnyCmt[] = (cmtCache?.pages ?? []).flatMap((p: any) => p?.comments ?? []);

  // ✔ 명시적 타입의 평탄화 헬퍼 (TS 경고 제거)
  const flattenComments = (nodes: AnyCmt[]): AnyCmt[] => {
    const out: AnyCmt[] = [];
    const dfs = (node: AnyCmt) => {
      const id = Number(node.id ?? node.commentId);
      const normalized: AnyCmt = {
        ...node,
        id,
        isDeleted:
          Number(node?.is_deleted ?? (node as any)?.isDeleted ?? 0) === 1 ||
          !!node?.isDeleted,
      };
      out.push(normalized);
      if (Array.isArray(node.replies)) node.replies.forEach((r) => dfs(r));
    };
    nodes.forEach((n) => dfs(n));
    return out;
  };

  const flat: AnyCmt[] = flattenComments(parents);

  const getTime = (c: AnyCmt) =>
    new Date((c.updatedAt ?? c.createdAt) ?? 0).getTime();

  const preview: AnyCmt[] = flat
    .slice()
    .sort((a: AnyCmt, b: AnyCmt) => getTime(a) - getTime(b))
    .slice(-2);

  return (
    <TouchableOpacity
      onPress={() => navigation.navigate('PostDetail', { postId: post.id })}
      activeOpacity={0.8}
    >
      <View style={styles.container}>
        {/* 우측 댓글 박스 */}
        <View style={styles.commentBox}>
          <Text style={styles.commentTextNumber}>{commentCount}</Text>
          <Text style={styles.commentTextLabel}>댓글</Text>
        </View>

        <Text style={styles.title} numberOfLines={1}>{post.title}</Text>

        <Text style={styles.meta} numberOfLines={1}>
          {post.nickname}   {new Date(post.createdAt).toLocaleString()}   👁 {views}{'  '}
          {liked ? '❤️' : '🤍'} {likeCount}
        </Text>

        {/* 댓글 미리보기 (캐시에 있을 때만) */}
        {preview.length > 0 && (
          <View style={styles.previewWrap}>
            {preview.map((c: AnyCmt) => {
              const nick = c.authorNickname ?? c.nickname ?? '익명';
              const text = c.isDeleted ? '(삭제된 댓글입니다)' : (c.content ?? '');
              return (
                <Text key={String(c.id)} style={styles.previewLine} numberOfLines={1}>
                  <Text style={styles.previewNick}>{nick}:</Text> {text}
                </Text>
              );
            })}
          </View>
        )}
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderColor: '#eee',
    position: 'relative',
    minHeight: 80,
    marginBottom: 3,
  },
  title: { fontWeight: 'bold', fontSize: 16, marginBottom: 4, paddingRight: 72 },
  meta: { fontSize: 12, color: '#888', paddingRight: 72 },

  // 우측 댓글 박스
  commentBox: {
    position: 'absolute',
    top: 8,
    right: 8,
    backgroundColor: '#f1f1f1',
    borderRadius: 12,
    width: 60,
    height: 60,
    justifyContent: 'center',
    alignItems: 'center',
  },
  commentTextNumber: { fontWeight: 'bold', fontSize: 16, color: '#333' },
  commentTextLabel: { fontSize: 13, color: '#555' },

  // 댓글 미리보기
  previewWrap: { marginTop: 6, paddingRight: 72 },
  previewLine: { fontSize: 12, color: '#444', marginTop: 2 },
  previewNick: { fontWeight: '600' },
});
