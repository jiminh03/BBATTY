// pages/post/PostListScreen.tsx
import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { FlatList, View, ActivityIndicator, Text } from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import { usePostListQuery } from '../../entities/post/queries/usePostQueries';
import { PostItem } from '../../entities/post/ui/PostItem';
import { useUserStore } from '../../entities/user/model/userStore';

export const PostListScreen = ({ route }: HomeStackScreenProps<'PostList'>) => {
  // teamId가 없으면 기본값 1
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

  // React Query 캐시에서 평탄화 (data 참조가 바뀔 때만 재계산)
  const posts = useMemo(
    () => (data?.pages ?? []).flatMap((p) => p?.posts ?? []),
    [data]
  );

  // 무한스크롤 보호(초기 진입 직후 onEndReached 과호출 방지)
  useEffect(() => {
    const timer = setTimeout(() => setCanLoadMore(true), 500);
    return () => clearTimeout(timer);
  }, []);

  const onEndReached = useCallback(() => {
    if (canLoadMore && hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  }, [canLoadMore, hasNextPage, isFetchingNextPage, fetchNextPage]);

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
        // ✅ 캐시가 바뀌면 강제로 리스트가 다시 그려지도록
        //    (syncLikeEverywhere가 ['posts', teamId] 캐시를 갱신하면 여기 값도 바뀜)
        extraData={data?.pages}
        renderItem={({ item }) => (
          // PostItem은 반드시 item.isLiked / item.likes를 사용해 렌더링해야 합니다.
          <PostItem post={item} />
        )}
        onEndReached={onEndReached}
        onEndReachedThreshold={0.1}
        ListFooterComponent={
          isFetchingNextPage ? (
            <View style={{ paddingVertical: 16 }}>
              <ActivityIndicator />
            </View>
          ) : null
        }
        // 스크롤 성능 튜닝(선택)
        initialNumToRender={8}
        windowSize={7}
        removeClippedSubviews
      />
    </View>
  );
};
