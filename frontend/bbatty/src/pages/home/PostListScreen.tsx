// pages/post/PostListScreen.tsx
import React from 'react';
import { FlatList, View, ActivityIndicator, Text } from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import { usePostListQuery } from '../../entities/post/queries/usePostQueries';
import { PostItem } from '../../entities/post/ui/PostItem';
import { useUserStore } from '../../entities/user/model/userStore';

export const PostListScreen = ({ route }: HomeStackScreenProps<'PostList'>) => {
  // teamId가 없으면 기본값 1로
  const storeTeamId = useUserStore((s) => s.currentUser?.teamId);
  const teamId = route.params?.teamId ?? storeTeamId ?? 1;

  const { data, fetchNextPage, hasNextPage, isLoading, isError, error } =
    usePostListQuery(teamId);

  const posts = data?.pages?.flatMap((p) => p?.posts ?? []) ?? [];



  if (isLoading)
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );

  if (isError)
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <Text>오류: {(error as Error).message}</Text>
      </View>
    );

  return (
    <View style={{ flex: 1 }}>
      <FlatList
        data={posts}
        keyExtractor={(item) => String(item.id)}
        renderItem={({ item }) => <PostItem post={item} />}
        onEndReached={() => {
          if (hasNextPage) fetchNextPage();
        }}
        onEndReachedThreshold={0.5}
      />
    </View>
  );
};
