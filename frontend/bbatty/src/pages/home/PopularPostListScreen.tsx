// pages/post/PopularPostsScreen.tsx (이름은 네가 쓴 파일명)
import React from 'react';
import { View, Text, ActivityIndicator, FlatList, StyleSheet } from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import { usePopularPostsQuery } from '../../entities/post/queries/usePostQueries';
import { PostItem } from '../../entities/post/ui/PostItem';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

type Props = HomeStackScreenProps<'PopularPosts'>;


export default function PopularPostsScreen({ route }: Props) {
  const insets = useSafeAreaInsets();
  const { teamId } = route.params;
  const { data = [], isLoading, isError } = usePopularPostsQuery(teamId);

  if (isLoading) return <ActivityIndicator style={{ marginTop: 20 }} />;
  if (isError) return <Text style={s.msg}>인기 글을 불러오지 못했어요.</Text>;

  const bottomPad = insets.bottom + 84; // 탭바 높이 대략 64~84, 기기별 여유 포함

  return (
    <FlatList
      data={data}
      keyExtractor={(p) => String(p.id)}
      renderItem={({ item }) => <PostItem post={item} />}
      contentContainerStyle={{ paddingTop: 8, paddingBottom: bottomPad }}
      ListFooterComponent={<View style={{ height: bottomPad }} />} // 안드로이드 대비
    />
  );
}

const s = StyleSheet.create({ msg: { marginTop: 16, textAlign: 'center', color: '#666' } });