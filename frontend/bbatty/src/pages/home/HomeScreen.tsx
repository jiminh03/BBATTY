// pages/home/HomeScreen.tsx
import React, { useMemo, useState, useCallback } from 'react';
import {
  View, FlatList, ActivityIndicator, StyleSheet, TouchableOpacity,
  Pressable, Text, TextInput,
} from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import TeamHeaderCard from '../../entities/team/ui/TeamHeaderCard';
import SegmentTabs from '../../entities/team/ui/SegmentTabs';
import { PostItem } from '../../entities/post/ui/PostItem';
import {
  useTeamPopularPostsQuery,
  usePostListQuery,
  useTeamSearchPostsInfinite,
} from '../../entities/post/queries/usePostQueries';
import { useUserStore } from '../../entities/user/model/userStore';
import TeamGearIcon from '../../shared/ui/atoms/Team/TeamGearIcon';
import { findTeamById } from '../../shared/team/teamTypes';
import { useTeamStanding } from '../../entities/team/queries/useTeamStanding';
import { useSearchHistoryStore } from '../../entities/post/model/searchHistoryStore';

type Props = HomeStackScreenProps<'Home'>;

function SearchHeader({
  keyword,
  onChangeKeyword,
  onSubmit,
  onClear,
  history,
  onPressChip,
  isSearching,
}: {
  keyword: string;
  onChangeKeyword: (v: string) => void;
  onSubmit: () => void;
  onClear: () => void;
  history: string[];
  onPressChip: (q: string) => void;
  isSearching: boolean;
}) {
  return (
    <View style={styles.searchSection}>
      <View style={styles.searchRow}>
        <TextInput
          style={styles.searchInput}
          placeholder="제목 및 내용으로 게시글을 검색해 보세요"
          value={keyword}
          onChangeText={onChangeKeyword}
          returnKeyType="search"
          onSubmitEditing={onSubmit}
        />
        {isSearching ? (
          <Pressable style={styles.clearBtn} onPress={onClear}>
            <Text style={styles.clearBtnText}>취소</Text>
          </Pressable>
        ) : (
          <Pressable style={styles.searchBtn} onPress={onSubmit}>
            <Text style={styles.searchBtnText}>검색</Text>
          </Pressable>
        )}
      </View>

      {history.length > 0 && !isSearching && (
        <View style={styles.chipWrap}>
          {history.map((q) => (
            <Pressable key={q} style={styles.chip} onPress={() => onPressChip(q)}>
              <Text style={styles.chipText}>{q}</Text>
            </Pressable>
          ))}
        </View>
      )}
    </View>
  );
}

export default function HomeScreen({ navigation }: Props) {
  const teamId = useUserStore((s) => s.currentUser?.teamId) ?? 1;
  const team = findTeamById(teamId);
  const teamColor = team?.color ?? '#1D467F';

  const { data: standing } = useTeamStanding(teamId);
  const rankText = standing ? `${standing.rank}위` : '순위 준비중';
  const recordText = standing
    ? `${standing.wins}승 ${standing.draws}무 ${standing.losses}패 (${standing.winRate.toFixed(3)})`
    : '전적 준비중';

  const [tab, setTab] = useState<'best' | 'all'>('all');

  // 전체/베스트
  const { data: popular = [], isLoading: pLoading } = useTeamPopularPostsQuery(teamId, 20);
  const listQ = usePostListQuery(teamId);
  const allPosts = useMemo(
    () => (listQ.data?.pages ?? []).flatMap((p) => p.posts ?? []),
    [listQ.data]
  );

  // 검색 상태 (페이지 이동 없이)
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState(''); // 제출된 검색어
  const addHistory = useSearchHistoryStore((s) => s.add);
  const history = useSearchHistoryStore((s) => s.byTeam[teamId] ?? []);
  const isSearching = submittedKeyword.length > 0;

  const searchQ = useTeamSearchPostsInfinite(teamId, submittedKeyword);
  const searchPosts = useMemo(
    () => (searchQ.data?.pages ?? []).flatMap((p) => p.posts ?? []),
    [searchQ.data]
  );

  const submitWith = useCallback(
    (q: string) => {
      const t = q.trim();
      if (!t) return;
      addHistory(teamId, t);
      setSubmittedKeyword(t); // ✅ 여기서 검색 모드로 전환
    },
    [addHistory, teamId]
  );

  const handleSubmit = useCallback(() => submitWith(keyword), [submitWith, keyword]);

  const handleClearSearch = useCallback(() => {
    setSubmittedKeyword('');
    setKeyword('');
  }, []);

  const listData = tab === 'best' ? popular : isSearching ? searchPosts : allPosts;
  const isFetchingNext =
    tab === 'all'
      ? isSearching
        ? searchQ.isFetchingNextPage
        : listQ.isFetchingNextPage
      : false;
  const hasNext =
    tab === 'all'
      ? isSearching
        ? searchQ.hasNextPage ?? false
        : listQ.hasNextPage ?? false
      : false;

  const fetchMore = () => {
    if (!hasNext || isFetchingNext) return;
    if (tab === 'all') {
      isSearching ? searchQ.fetchNextPage() : listQ.fetchNextPage();
    }
  };

  return (
    <View style={{ flex: 1, backgroundColor: '#fff' }}>
      <TeamHeaderCard
        teamLogo={String(team?.imagePath ?? '')}
        teamName={team?.name ?? 'KBO 팀'}
        rankText={rankText}
        recordText={recordText}
        accentColor={teamColor}
      />

      <SegmentTabs value={tab} onChange={setTab} />

      {tab === 'best' ? (
        pLoading ? (
          <ActivityIndicator style={{ marginTop: 16 }} />
        ) : (
          <FlatList
            data={listData}
            keyExtractor={(i) => String(i.id)}
            renderItem={({ item }) => (
              <PostItem
                post={item}
                onPress={() => navigation.navigate('PostDetail', { postId: item.id })}
              />
            )}
            contentContainerStyle={styles.listPad}
          />
        )
      ) : (
        <FlatList
          data={listData}
          keyExtractor={(i) => String(i.id)}
          renderItem={({ item }) => (
            <PostItem
              post={item}
              onPress={() => navigation.navigate('PostDetail', { postId: item.id })}
            />
          )}
          ListHeaderComponent={
            <SearchHeader
              keyword={keyword}
              onChangeKeyword={setKeyword}
              onSubmit={handleSubmit}
              onClear={handleClearSearch}
              history={history}
              onPressChip={(q) => {
                setKeyword(q);
                submitWith(q);
              }}
              isSearching={isSearching}
            />
          }
          onEndReachedThreshold={0.35}
          onEndReached={fetchMore}
          ListFooterComponent={
            isFetchingNext ? <ActivityIndicator style={{ marginVertical: 12 }} /> : <View />
          }
          contentContainerStyle={styles.listPad}
        />
      )}

      <TouchableOpacity
        style={styles.fab}
        onPress={() => navigation.navigate('PostForm' as never)}
        activeOpacity={0.85}
        accessibilityLabel="게시글 작성"
      >
        <View style={styles.fabCircle}>
          <TeamGearIcon teamId={teamId} size={44} />
        </View>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  listPad: { paddingBottom: 16 },

  searchSection: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8, backgroundColor: '#fff' },
  searchRow: { flexDirection: 'row', gap: 8 },
  searchInput: {
    flex: 1,
    height: 44,
    borderRadius: 10,
    backgroundColor: '#F5F6F7',
    paddingHorizontal: 12,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#E3E5E7',
  },
  searchBtn: {
    height: 44,
    paddingHorizontal: 14,
    borderRadius: 10,
    backgroundColor: '#E95F2E',
    alignItems: 'center',
    justifyContent: 'center',
  },
  searchBtnText: { color: '#fff', fontWeight: '700' },

  clearBtn: {
    height: 44,
    paddingHorizontal: 14,
    borderRadius: 10,
    backgroundColor: '#D1D5DB',
    alignItems: 'center',
    justifyContent: 'center',
  },
  clearBtnText: { color: '#111', fontWeight: '700' },

  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 10 },
  chip: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    backgroundColor: '#F1F3F4',
    borderRadius: 16,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#E3E5E7',
  },
  chipText: { color: '#5F6368', fontSize: 12 },

  fab: { position: 'absolute', right: 16, bottom: 24 },
  fabCircle: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOpacity: 0.15,
    shadowOffset: { width: 0, height: 4 },
    shadowRadius: 8,
    elevation: 6,
  },
});
