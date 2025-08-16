// pages/post/PostListScreen.tsx (전체 파일 대체)
import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { FlatList, View, ActivityIndicator, Text } from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import { usePostListQuery } from '../../entities/post/queries/usePostQueries';
import { PostItem } from '../../entities/post/ui/PostItem';
import { useUserStore } from '../../entities/user/model/userStore';
import { useFocusEffect } from '@react-navigation/native';
import { useQueryClient } from '@tanstack/react-query';

export const PostListScreen = ({ route }: HomeStackScreenProps<'PostList'>) => {
  const storeTeamId = useUserStore((s) => s.currentUser?.teamId);
  const teamId = route.params?.teamId ?? storeTeamId ?? 1;

  const [canLoadMore, setCanLoadMore] = useState(false);

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isLoading,
    isError,
    error,
    isFetchingNextPage,
  } = usePostListQuery(teamId);

  // 캐시 → 평탄화
  const posts = useMemo(
    () => (data?.pages ?? []).flatMap((p) => p?.posts ?? []),
    [data?.pages]
  );

  useEffect(() => {
    const t = setTimeout(() => setCanLoadMore(true), 400);
    return () => clearTimeout(t);
  }, []);

  const onEndReached = useCallback(() => {
    if (canLoadMore && hasNextPage && !isFetchingNextPage) fetchNextPage();
  }, [canLoadMore, hasNextPage, isFetchingNextPage, fetchNextPage]);

  // ✅ 목록 화면 포커스 시 가볍게 재검증(선택)
  const qc = useQueryClient();
  useFocusEffect(
    useCallback(() => {
      // 상세에서 낙관 반영은 이미 되어 있음. 여기서는 서버 정합성만 맞춤.
      qc.invalidateQueries({ queryKey: ['posts', teamId], refetchType: 'active' });
      // 인기/검색도 필요하면 아래 주석 해제
      // qc.invalidateQueries({ queryKey: ['popularPostsAll', teamId], refetchType: 'active' });
      // qc.invalidateQueries({ queryKey: ['popularPostsPreview', teamId], refetchType: 'active' });
      return () => {};
    }, [qc, teamId])
  );

  if (isLoading) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (isError) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <Text>오류: {(error as Error).message}</Text>
      </View>
    );
  }

  return (
    <View style={{ flex: 1 }}>
      <FlatList
        data={posts}
        keyExtractor={(item) => String(item.id)}
        // 캐시가 바뀌면 리렌더 보장
        extraData={data?.pages}
        renderItem={({ item }) => <PostItem post={item as any} teamId={teamId} />}
        onEndReached={onEndReached}
        onEndReachedThreshold={0.15}
        initialNumToRender={8}
        windowSize={7}
        removeClippedSubviews
      />
    </View>
  );
};

export default PostListScreen;
