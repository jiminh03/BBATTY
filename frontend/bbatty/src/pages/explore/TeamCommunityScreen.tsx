// pages/explore/TeamCommunityScreen.tsx
import React, { useMemo, useState, useEffect } from 'react';
import {
  View, FlatList, ActivityIndicator, StyleSheet,
  ScrollView, TouchableOpacity, Image, Text,
} from 'react-native';
import { ExploreStackScreenProps } from '../../navigation/types';
import { usePostListQuery } from '../../entities/post/queries/usePostQueries';
import { PostItem } from '../../entities/post/ui/PostItem';
import { useUserStore } from '../../entities/user/model/userStore';
import { TEAMS } from '../../shared/team/teamTypes';

type Props = ExploreStackScreenProps<'TeamCommunity'>;

export default function TeamCommunityScreen({ navigation, route }: Props) {
  const myTeamId = useUserStore(s => s.currentUser?.teamId);

  // 내 팀은 숨기기
  const teamOptions = useMemo(
    () => TEAMS.filter(t => t.id !== (myTeamId ?? -1)),
    [myTeamId]
  );

  // 선택 상태를 teamId로 관리 (이게 핵심!)
  const initialTeamId =
    route.params?.teamId && teamOptions.some(t => t.id === route.params!.teamId)
      ? route.params!.teamId
      : (teamOptions[0]?.id ?? 1);

  const [selectedTeamId, setSelectedTeamId] = useState<number>(initialTeamId);

  // 라우트나 옵션 변경 시 selectedTeamId 보정
  useEffect(() => {
    if (route.params?.teamId && teamOptions.some(t => t.id === route.params!.teamId)) {
      setSelectedTeamId(route.params!.teamId);
    } else if (!teamOptions.some(t => t.id === selectedTeamId)) {
      // 현재 선택이 옵션에서 사라졌다면 첫 번째로 보정
      if (teamOptions[0]?.id) setSelectedTeamId(teamOptions[0].id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [route.params?.teamId, teamOptions]);

  // 팀별 무한 스크롤
  const listQ = usePostListQuery(selectedTeamId);
  const posts = useMemo(
    () => (listQ.data?.pages ?? []).flatMap(p => p?.posts ?? []),
    [listQ.data]
  );

  const TeamPicker = () => (
    <View style={styles.teamPickerWrap}>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.teamPickerContent}
      >
        {teamOptions.map(t => {
          const active = t.id === selectedTeamId;
          return (
            <TouchableOpacity
              key={t.id}
              style={[styles.teamChip, active && styles.teamChipActive]}
              onPress={() => setSelectedTeamId(t.id)}
              activeOpacity={0.85}
            >
              <Image
                source={typeof t.imagePath === 'string' ? { uri: t.imagePath } : t.imagePath}
                style={styles.teamLogo}
                resizeMode="contain"
              />
              <Text style={[styles.teamChipLabel, active && { color: t.color }]}>
                {t.name.split(' ')[0]}
              </Text>
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    </View>
  );

  return (
    <View style={styles.container}>
      <TeamPicker />

      {listQ.isLoading ? (
        <View style={styles.center}><ActivityIndicator size="large" /></View>
      ) : listQ.isError ? (
        <View style={styles.center}>
          <Text style={styles.empty}>불러오는 중 문제가 발생했어요.</Text>
        </View>
      ) : (
        <FlatList
          key={selectedTeamId}  // 팀 변경 시 깔끔 리마운트
          data={posts}
          keyExtractor={i => String(i.id)}
          renderItem={({ item }) => (
            <PostItem
              post={item}
              onPress={() =>
                navigation.navigate('PostDetail', { postId: item.id })
              }
            />
          )}
          onEndReachedThreshold={0.35}
          onEndReached={() => {
            if (listQ.hasNextPage && !listQ.isFetchingNextPage) listQ.fetchNextPage();
          }}
          ListFooterComponent={
            listQ.isFetchingNextPage ? <ActivityIndicator style={{ marginVertical: 12 }} /> : <View />
          }
          ListEmptyComponent={
            !listQ.isFetching && posts.length === 0
              ? <Text style={styles.empty}>게시글이 없어요.</Text>
              : null
          }
          contentContainerStyle={{ paddingBottom: 16 }}
        />
      )}
    </View>
  );
}

const CHIP_H = 64;
const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  teamPickerWrap: {
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#eee',
    backgroundColor: '#fff',
  },
  teamPickerContent: { paddingHorizontal: 12, paddingVertical: 10, gap: 10 },
  teamChip: {
    height: CHIP_H, minWidth: 86, paddingHorizontal: 10, paddingVertical: 8,
    backgroundColor: '#F6F7F8', borderRadius: 12,
    borderWidth: StyleSheet.hairlineWidth, borderColor: '#E3E5E7',
    alignItems: 'center', justifyContent: 'center',
  },
  teamChipActive: {
    backgroundColor: '#fff', borderColor: '#cfd3d7',
    shadowColor: '#000', shadowOpacity: 0.08, shadowOffset: { width: 0, height: 2 },
    shadowRadius: 4, elevation: 3,
  },
  teamLogo: { width: 36, height: 36, marginBottom: 4 },
  teamChipLabel: { fontSize: 12, color: '#666', fontWeight: '600' },
  empty: { textAlign: 'center', color: '#999', marginTop: 24 },
});
