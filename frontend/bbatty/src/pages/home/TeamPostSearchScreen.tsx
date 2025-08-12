import React, { useMemo, useState, useEffect } from 'react';
import {
  View, Text, TextInput, StyleSheet, FlatList, ActivityIndicator, Pressable
} from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import { useTeamPostSearchInfinite } from '../../entities/post/queries/usePostQueries';
import { PostItem } from '../../entities/post/ui/PostItem';

type Props = HomeStackScreenProps<'TeamPostSearch'>; // 네비게이션 타입에 추가 필요

export default function TeamPostSearchScreen({ route, navigation }: Props) {
  const { teamId, initialKeyword = '' } = route.params ?? { teamId: 0, initialKeyword: '' };

  const [keyword, setKeyword] = useState(initialKeyword);
  const [debounced, setDebounced] = useState(initialKeyword);

  // 입력 디바운스
  useEffect(() => {
    const t = setTimeout(() => setDebounced(keyword), 350);
    return () => clearTimeout(t);
  }, [keyword]);

  const {
    data, isLoading, isError, fetchNextPage, hasNextPage, isFetchingNextPage,
  } = useTeamPostSearchInfinite(teamId, debounced);

  const list = useMemo(
    () => (data?.pages ?? []).flatMap(p => p.posts ?? []),
    [data]
  );

  return (
    <View style={s.container}>
      <View style={s.searchBar}>
        <TextInput
          style={s.input}
          value={keyword}
          onChangeText={setKeyword}
          placeholder="검색어를 입력하세요"
          autoFocus
          returnKeyType="search"
        />
        {!!keyword && (
          <Pressable onPress={() => setKeyword('')} style={s.clear}>
            <Text style={{ fontSize: 16 }}>✕</Text>
          </Pressable>
        )}
      </View>

      {!debounced ? (
        <Text style={s.helper}>검색어를 입력하면 결과가 표시됩니다.</Text>
      ) : isLoading ? (
        <ActivityIndicator style={{ marginTop: 16 }} />
      ) : isError ? (
        <Text style={s.helper}>검색 결과를 불러오지 못했어요.</Text>
      ) : list.length === 0 ? (
        <Text style={s.helper}>결과가 없어요.</Text>
      ) : (
        <FlatList
          data={list}
          keyExtractor={(p) => String(p.id)}
          renderItem={({ item }) => <PostItem post={item} />}
          onEndReachedThreshold={0.4}
          onEndReached={() => hasNextPage && fetchNextPage()}
          ListFooterComponent={
            isFetchingNextPage ? <ActivityIndicator style={{ marginVertical: 12 }} /> : <View />
          }
        />
      )}
    </View>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderColor: '#eee',
  },
  input: {
    flex: 1, borderWidth: 1, borderColor: '#ddd', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 8,
  },
  clear: { marginLeft: 8, padding: 8 },
  helper: { marginTop: 16, textAlign: 'center', color: '#666' },
});
