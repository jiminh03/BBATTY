// pages/home/HomeScreen.tsx
import React, { useMemo, useState, useCallback, useEffect } from 'react';
import {
  View,
  FlatList,
  ActivityIndicator,
  StyleSheet,
  TouchableOpacity,
  Pressable,
  Text,
  TextInput,
  Alert,
  LayoutAnimation,
  UIManager,
  Platform,
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
import { useAttendanceStore } from '../../entities/attendance/model/attendanceStore';
import TeamGearIcon from '../../shared/ui/atoms/Team/TeamGearIcon';
import { findTeamById, getTeamInfo } from '../../shared/team/teamTypes';
import { useTeamStanding } from '../../entities/team/queries/useTeamStanding';
import { useSearchHistoryStore } from '../../entities/post/model/searchHistoryStore';
import { chatRoomApi } from '../../entities/chat-room/api/api';
import { gameApi } from '../../entities/game/api/api';
import TeamNewsSection from '../../entities/post/ui/TeamNewsSection';

if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

type Props = HomeStackScreenProps<'Home'>;
const ACTIONS_TOP = Platform.select({ android: 96, ios: 102 }); // 버튼 세로 위치

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
  const { isVerifiedToday } = useAttendanceStore();
  const team = findTeamById(teamId);
  const teamColor = team?.color ?? '#1D467F';

  const { data: standing } = useTeamStanding(teamId);
  const rankText = standing ? `${standing.rank}위` : '순위 준비중';
  const recordText = standing
    ? `${standing.wins}승 ${standing.draws}무 ${standing.losses}패 (${standing.winRate.toFixed(3)})`
    : '전적 준비중';

  const [tab, setTab] = useState<'best' | 'all'>('all');

  // 헤더 안에서 펼칠 팀 최신 뉴스 상태 (부모가 제어)
  const [newsOpen, setNewsOpen] = useState(false);
  const toggleNews = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setNewsOpen((v) => !v);
  };
  // 탭 바뀌면 접기 (안드릭 addView index 에러 예방)
  useEffect(() => {
    if (newsOpen) setNewsOpen(false);
  }, [tab]);

  // 전체/베스트
  const { data: popular = [], isLoading: pLoading } = useTeamPopularPostsQuery(teamId, 20);
  const listQ = usePostListQuery(teamId);
  const allPosts = useMemo(
    () => (listQ.data?.pages ?? []).flatMap((p) => p.posts ?? []),
    [listQ.data]
  );

  // 검색 상태
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const addHistory = useSearchHistoryStore((s) => s.add);
  const getHistoryForTeam = useSearchHistoryStore((s) => s.getHistoryForTeam);
  const history = getHistoryForTeam(teamId);
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
      setSubmittedKeyword(t);
    },
    [addHistory, teamId]
  );

  const handleSubmit = useCallback(() => submitWith(keyword), [submitWith, keyword]);
  const handleClearSearch = useCallback(() => {
    setSubmittedKeyword('');
    setKeyword('');
  }, []);

  const handleChatPress = async () => {
    const verified = isVerifiedToday();
    if (!verified) {
      navigation.navigate('AttendanceVerification' as never);
      return;
    }

    try {
      const currentUser = useUserStore.getState().currentUser;
      if (!currentUser) {
        Alert.alert('오류', '사용자 정보를 찾을 수 없습니다.');
        return;
      }

      const todayGameResponse = await gameApi.getTodayGame();
      if (todayGameResponse.status !== 'SUCCESS' || !todayGameResponse.data) {
        Alert.alert('오류', '오늘의 경기 정보를 가져올 수 없습니다.');
        return;
      }
      const todayGame = todayGameResponse.data;

      const watchRequest = {
        gameId: todayGame.gameId,
        teamId: currentUser.teamId,
        isAttendanceVerified: true,
      };

      const response = await chatRoomApi.joinWatchChat(watchRequest);
      if (response.data.status !== 'SUCCESS') {
        Alert.alert(
          '연결 실패',
          response.data.message || JSON.stringify(response.data) || '직관채팅 연결에 실패했습니다.'
        );
        return;
      }

      const gameDetails = await gameApi.getGameById(todayGame.gameId.toString());
      if (!gameDetails || gameDetails.status !== 'SUCCESS') {
        Alert.alert('오류', '게임 정보를 불러올 수 없습니다.');
        return;
      }

      navigation.navigate('ChatStack', {
        screen: 'MatchChatRoom',
        params: {
          room: {
            matchId: `watch_chat_${todayGame.gameId}_${currentUser.teamId}`,
            gameId: todayGame.gameId.toString(),
            matchTitle: `직관채팅 - ${gameDetails.data.awayTeamName} vs ${gameDetails.data.homeTeamName}`,
            matchDescription: `${gameDetails.data.stadium}에서 열리는 경기를 함께 시청하며 채팅하는 공간`,
            teamId: getTeamInfo(currentUser.teamId).name,
            minAge: 0,
            maxAge: 100,
            genderCondition: 'ALL',
            maxParticipants: 999,
            currentParticipants: 0,
            createdAt: new Date().toISOString(),
            status: 'ACTIVE',
            websocketUrl: response.data.data.websocketUrl,
          },
          websocketUrl: response.data.data.websocketUrl,
          sessionToken: response.data.data.sessionToken,
        },
      });
    } catch (e) {
      console.error('직관채팅 연결 중 오류:', e);
      Alert.alert('오류', '직관채팅 연결 중 문제가 발생했습니다.');
    }
  };

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
    if (tab === 'all') (isSearching ? searchQ.fetchNextPage() : listQ.fetchNextPage());
  };

  return (
    <View style={{ flex: 1, backgroundColor: '#fff' }}>
      {/* 헤더(팀색 배경) */}
      <View style={[styles.headerWrap, { backgroundColor: teamColor }]}>
        {/* 팀 카드 – 살짝 아래 여백 주기 */}
        <View style={{ paddingTop: 8 }}>
          <TeamHeaderCard
            teamLogo={String(team?.imagePath ?? '')}
            teamName={team?.name ?? 'KBO 팀'}
            rankText={rankText}
            recordText={recordText}
            onPressChat={handleChatPress}
            accentColor={teamColor}
          />
        </View>

        {/* 오른쪽에 나란히 떠 있는 알약 버튼 두 개 */}
        <View style={[styles.actionRow, { top: ACTIONS_TOP }]}>
          {/* <TouchableOpacity onPress={handleChatPress} activeOpacity={0.9} style={styles.pill}>
            <Text style={[styles.pillText, { color: teamColor }]}>직관인증하기</Text>
          </TouchableOpacity> */}

          <TouchableOpacity onPress={toggleNews} activeOpacity={0.9} style={styles.pill}>
            <Text style={[styles.pillText, { color: 'black' }]}>
              팀 최신 뉴스 {newsOpen ? '▴' : '▾'}
            </Text>
          </TouchableOpacity>
        </View>

        {/* 헤더 아래에 즉시 펼쳐지는 뉴스 영역 */}
        {newsOpen && (
          <View style={{ paddingTop: 8, paddingBottom: 8 }}>
            <TeamNewsSection
              teamId={teamId}
              titleColor="#fff"
              accentColor={teamColor}
              expanded={true}
              showHeader={false}               // 헤더 버튼을 사용 중이므로 내부 타이틀 숨김
              style={{ backgroundColor: 'transparent' }}
            />
          </View>
        )}
      </View>

      {/* 탭 */}
      <SegmentTabs value={tab} onChange={setTab} />

      {/* 리스트 */}
      {tab === 'best' ? (
        pLoading ? (
          <ActivityIndicator style={{ marginTop: 16 }} />
        ) : (
          <FlatList
            key={tab}
            data={listData}
            keyExtractor={(i) => String(i.id)}
            renderItem={({ item }) => (
              <PostItem
                post={item}
                onPress={() => navigation.navigate('PostDetail', { postId: item.id })}
              />
            )}
            ListHeaderComponent={<View />}      // 항상 단일 View
            removeClippedSubviews={false}
            contentContainerStyle={styles.listPad}
          />
        )
      ) : (
        <FlatList
          key={tab}
          data={listData}
          keyExtractor={(i) => String(i.id)}
          renderItem={({ item }) => (
            <PostItem
              post={item}
              onPress={() => navigation.navigate('PostDetail', { postId: item.id })}
            />
          )}
          ListHeaderComponent={
            <View>
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
            </View>
          }
          onEndReachedThreshold={0.35}
          onEndReached={fetchMore}
          removeClippedSubviews={false}
          ListFooterComponent={
            isFetchingNext ? <ActivityIndicator style={{ marginVertical: 12 }} /> : <View />
          }
          contentContainerStyle={styles.listPad}
        />
      )}

      {/* 새 글 FAB */}
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

  headerWrap: {
    position: 'relative',
    paddingBottom: 12,
  },
  actionRow: {
    position: 'absolute',
    right: 16,
    flexDirection: 'row',
  },
  pill: {
    height: 33,
    paddingHorizontal: 10,
    borderRadius: 10,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    // shadowColor: '#000',
    // shadowOpacity: 0.12,
    // shadowOffset: { width: 0, height: 2 },
    // shadowRadius: 6,
    // elevation: 3,
    marginLeft: 8,
    marginRight: 12,
  },
  pillText: { fontSize: 11.5, fontWeight: '700' },

  // 검색 UI
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

  // FAB
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
