// pages/explore/TeamCommunityScreen.tsx
import React, { useMemo, useState, useEffect, useRef } from 'react';
import {
  View, FlatList, ActivityIndicator, StyleSheet,
  ScrollView, TouchableOpacity, Image, Text,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { ExploreStackScreenProps } from '../../navigation/types';
import { usePostListQuery } from '../../entities/post/queries/usePostQueries';
import { PostItem } from '../../entities/post/ui/PostItem';
import { useUserStore } from '../../entities/user/model/userStore';
import { TEAMS } from '../../shared/team/teamTypes';

type Props = ExploreStackScreenProps<'TeamCommunity'>;

const STORAGE_KEY = 'teamCommunity_selectedTeam';

export default function TeamCommunityScreen({ navigation, route }: Props) {
  const myTeamId = useUserStore(s => s.currentUser?.teamId);
  const scrollViewRef = useRef<ScrollView>(null);
  const scrollPositionRef = useRef(0);

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

  // 선택된 팀 상태를 로컬 스토리지에서 불러오기
  useEffect(() => {
    const loadSelectedTeam = async () => {
      try {
        const savedTeamId = await AsyncStorage.getItem(STORAGE_KEY);
        if (savedTeamId) {
          const teamId = parseInt(savedTeamId);
          setSelectedTeamId(teamId);
        }
      } catch (error) {
        console.error('선택된 팀 로드 실패:', error);
      }
    };
    loadSelectedTeam();
  }, []); // 빈 의존성 배열로 변경

  // 라우트 변경 시 selectedTeamId 보정
  useEffect(() => {
    if (route.params?.teamId) {
      setSelectedTeamId(route.params.teamId);
    }
  }, [route.params?.teamId]); // teamOptions 의존성 제거

  // 팀 변경 시 로컬 스토리지에 저장 (스크롤 위치 건드리지 않음)
  const handleTeamChange = async (teamId: number) => {
    try {
      setSelectedTeamId(teamId);
      await AsyncStorage.setItem(STORAGE_KEY, teamId.toString());
      
      // 스크롤 위치 복원
      setTimeout(() => {
        scrollViewRef.current?.scrollTo({ x: scrollPositionRef.current, animated: false });
      }, 100);
    } catch (error) {
      console.error('선택된 팀 저장 실패:', error);
    }
  };


  // 팀별 무한 스크롤
  const listQ = usePostListQuery(selectedTeamId);
  const posts = useMemo(
    () => (listQ.data?.pages ?? []).flatMap(p => p?.posts ?? []),
    [listQ.data]
  );

  const [refreshing, setRefreshing] = useState(false);
  const handleRefresh = async () => {
    try {
      setRefreshing(true);
      await listQ.refetch();
    } finally {
      setRefreshing(false);
    }
  };

  const TeamPicker = () => (
    <View style={styles.teamPickerWrap}>
      <ScrollView
        ref={scrollViewRef}
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.teamPickerContent}
        onScroll={(event) => {
          scrollPositionRef.current = event.nativeEvent.contentOffset.x;
        }}
        scrollEventThrottle={16}
      >
        {teamOptions.map(t => {
          const active = t.id === selectedTeamId;
          return (
            <TouchableOpacity
              key={t.id}
              style={[styles.teamChip, active && styles.teamChipActive]}
              onPress={() => handleTeamChange(t.id)}
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
          data={posts}
          keyExtractor={i => String(i.id)}
          renderItem={({ item }) => (
            <PostItem
              post={item}
              teamId={selectedTeamId}
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
          refreshing={refreshing}
          onRefresh={handleRefresh}
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
  teamPickerContent: { paddingHorizontal: 12, paddingVertical: 10, gap: 10, justifyContent: 'center' },
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
