import React, { useMemo, useState, useCallback } from 'react';
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
  ImageSourcePropType,
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

// #RRGGBBAA → #RRGGBB
const normalizeHex = (hex?: string, fallback = '#1D467F') => {
  if (!hex) return fallback;
  if (hex.startsWith('#') && hex.length === 9) return hex.slice(0, 7);
  return hex;
};

// Zustand selectors
const selectTeamId = (state: any) => state.currentUser?.teamId ?? 1;
const selectIsVerifiedToday = (state: any) => state.isVerifiedToday;
const selectAddHistory = (state: any) => state.addHistory;
const selectRemoveHistory = (state: any) => state.removeHistory;
const selectGetHistoryForTeam = (state: any) => state.getHistoryForTeam;
const selectHistoriesByTeam = (state: any) => state.historiesByTeam;

/** ====== 검색 헤더 ====== */
function SearchHeader({
  keyword,
  onChangeKeyword,
  onSubmit,
  onClear,
  history,
  onPressChip,
  onRemoveChip,
  isSearching,
  accentColor,
}: {
  keyword: string;
  onChangeKeyword: (v: string) => void;
  onSubmit: () => void;
  onClear: () => void;
  history: string[];
  onPressChip: (q: string) => void;
  onRemoveChip: (q: string) => void;
  isSearching: boolean;
  accentColor: string;
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
          <Pressable
            style={[styles.searchBtn, { backgroundColor: accentColor }]}
            onPress={onSubmit}
          >
            <Text style={styles.searchBtnText}>검색</Text>
          </Pressable>
        )}
      </View>

      {history.length > 0 && !isSearching && (
        <View style={styles.chipWrap}>
          {history.map((q) => (
            <View key={q} style={styles.chip}>
              <Pressable
                onPress={() => onPressChip(q)}
                style={styles.chipLabelBtn}
                android_ripple={{ color: '#e5e7eb' }}
              >
                <Text style={styles.chipText} numberOfLines={1}>
                  {q}
                </Text>
              </Pressable>
              <Pressable
                onPress={() => onRemoveChip(q)}
                hitSlop={8}
                style={styles.chipClose}
                android_ripple={{ color: '#e5e7eb', borderless: true }}
                accessibilityLabel={`${q} 제거`}
              >
                <Text style={styles.chipCloseText}>×</Text>
              </Pressable>
            </View>
          ))}
        </View>
      )}
    </View>
  );
}

/** ====== 메인 ====== */
function HomeScreen({ navigation }: Props) {
  const teamId = useUserStore(selectTeamId);
  const isVerifiedToday = useAttendanceStore(selectIsVerifiedToday);
  const team = findTeamById(teamId);
  const teamColor = normalizeHex(team?.color, '#1D467F');

  const { data: standing } = useTeamStanding(teamId);
  const rankText = standing ? `${standing.rank}위` : '순위 준비중';
  const recordText = standing
    ? `${standing.wins}승 ${standing.draws}무 ${standing.losses}패 (${standing.winRate.toFixed(3)})`
    : '전적 준비중';

  const [tab, setTab] = useState<'best' | 'all'>('all');

  // 뉴스 토글(버튼으로만)
  const [newsOpen, setNewsOpen] = useState(false);
  const toggleNews = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setNewsOpen((v) => !v);
  };

  // 데이터
  const { data: popular = [], isLoading: pLoading } = useTeamPopularPostsQuery(teamId, 20);
  const listQ = usePostListQuery(teamId);
  const allPosts = useMemo(
    () => (listQ.data?.pages ?? []).flatMap((p) => p.posts ?? []),
    [listQ.data]
  );

  // 검색
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const addHistory = useSearchHistoryStore(selectAddHistory);
  const removeHistory = useSearchHistoryStore(selectRemoveHistory);
  const getHistoryForTeam = useSearchHistoryStore(selectGetHistoryForTeam);
  const historiesByTeam = useSearchHistoryStore(selectHistoriesByTeam);
  const history = historiesByTeam[teamId] ?? [];

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
  const handleRemoveChip = useCallback((q: string) => {
    removeHistory(teamId, q);
  }, [removeHistory, teamId]);

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
        ? (searchQ.hasNextPage ?? false)
        : (listQ.hasNextPage ?? false)
      : false;

  const fetchMore = () => {
    if (!hasNext || isFetchingNext) return;
    if (tab === 'all') (isSearching ? searchQ.fetchNextPage() : listQ.fetchNextPage());
  };

  // 직관채팅 버튼
  const handleChatPress = useCallback(async () => {
    const verified = isVerifiedToday();
    if (verified) {
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
        if (response.data.status === 'SUCCESS') {
          const gameDetails = await gameApi.getGameById(todayGame.gameId.toString());
          if (!gameDetails || gameDetails.status !== 'SUCCESS') {
            Alert.alert('오류', '게임 정보를 불러올 수 없습니다.');
            return;
          }

          const watchChatRoom = {
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
          };

          (navigation as any).navigate('WatchChatModal', {
            room: watchChatRoom,
            websocketUrl: response.data.data.websocketUrl,
            sessionToken: response.data.data.sessionToken,
          });
        } else {
          Alert.alert('연결 실패', response.data.message || JSON.stringify(response.data) || '직관채팅 연결에 실패했습니다.');
        }
      } catch (e) {
        console.error('직관채팅 연결 오류:', e);
        Alert.alert('오류', '직관채팅 연결 중 문제가 발생했습니다.');
      }
    } else {
      (navigation as any).navigate('AttendanceVerification');
    }
  }, [isVerifiedToday, navigation]);

  const teamLogoSrc: ImageSourcePropType | undefined = useMemo(
    () =>
      team?.imagePath
        ? (typeof team.imagePath === 'string' ? { uri: team.imagePath } : (team.imagePath as any))
        : undefined,
    [team?.imagePath]
  );

  return (
    <View style={{ flex: 1, backgroundColor: '#fff' }}>
      {/* 헤더(팀색 배경) */}
      <View style={[styles.headerWrap, { backgroundColor: teamColor }]}>
        <View style={{ paddingTop: 8 }}>
          <TeamHeaderCard
            teamLogo={teamLogoSrc}
            teamName={team?.name ?? 'KBO 팀'}
            rankText={rankText}
            recordText={recordText}
            onPressChat={handleChatPress}
            accentColor={teamColor}
            // ⭐ 우측 고정 영역에 “팀 최신 뉴스” 버튼을 꽂아줌 (하드코딩 top 제거!)
            rightExtras={
              <TouchableOpacity
                onPress={toggleNews}
                activeOpacity={0.9}
                style={{
                  minHeight: 38,
                  paddingHorizontal: 14,
                  borderRadius: 10,
                  backgroundColor: '#fff',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Text style={{ fontSize: 14, fontWeight: '600', color: '#000' }}>
                  팀 최신 뉴스
                </Text>
              </TouchableOpacity>
            }
          />
        </View>

        {/* 헤더 아래 뉴스(토글형) */}
        {newsOpen && (
          <View style={{ paddingTop: 8, paddingBottom: 8 }}>
            <TeamNewsSection
              teamId={teamId}
              titleColor="#fff"
              accentColor={teamColor}
              expanded={true}
              showHeader={false}
              style={{ backgroundColor: 'transparent' }}
            />
          </View>
        )}
      </View>

      {/* 탭 */}
      <SegmentTabs value={tab} onChange={setTab} accentColor={teamColor} />

      {/* 리스트 */}
      {tab === 'best' ? (
        pLoading ? (
          <ActivityIndicator style={{ marginTop: 16 }} />
        ) : (
          <FlatList
            key={tab}
            data={popular}
            keyExtractor={(i) => String(i.id)}
            renderItem={({ item }) => (
              <PostItem
                post={item}
                onPress={() => navigation.navigate('PostDetail', { postId: item.id })}
              />
            )}
            ListHeaderComponent={<View />}
            removeClippedSubviews={false}
            contentContainerStyle={styles.listPad}
          />
        )
      ) : (
        <FlatList
          key={tab}
          data={isSearching ? searchPosts : allPosts}
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
                  addHistory(teamId, q);
                  setKeyword(q);
                  setSubmittedKeyword(q);
                }}
                onRemoveChip={handleRemoveChip}
                isSearching={isSearching}
                accentColor={teamColor}
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
        onPress={() => navigation.navigate('PostForm')}
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

  headerWrap: { position: 'relative', paddingBottom: 12 },

  // 검색 UI
  searchSection: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8, backgroundColor: '#fff' },
  searchRow: { flexDirection: 'row', gap: 8 },
  searchInput: {
    flex: 1, height: 44, borderRadius: 10, backgroundColor: '#F5F6F7',
    paddingHorizontal: 12, borderWidth: StyleSheet.hairlineWidth, borderColor: '#E3E5E7',
  },
  searchBtn: {
    height: 44, paddingHorizontal: 14, borderRadius: 10,
    alignItems: 'center', justifyContent: 'center',
  },
  searchBtnText: { color: '#fff', fontWeight: '700' },
  clearBtn: {
    height: 44, paddingHorizontal: 14, borderRadius: 10, backgroundColor: '#D1D5DB',
    alignItems: 'center', justifyContent: 'center',
  },
  clearBtnText: { color: '#111', fontWeight: '700' },

  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 10 },
  chip: {
    flexDirection: 'row', alignItems: 'center',
    maxWidth: '100%',
    paddingLeft: 10, paddingRight: 6, paddingVertical: 6,
    backgroundColor: '#F1F3F4',
    borderRadius: 16,
    borderWidth: StyleSheet.hairlineWidth, borderColor: '#E3E5E7',
  },
  chipLabelBtn: { maxWidth: 180, paddingRight: 6 },
  chipText: { color: '#5F6368', fontSize: 12 },
  chipClose: {
    width: 18, height: 18, borderRadius: 9,
    alignItems: 'center', justifyContent: 'center',
  },
  chipCloseText: { fontSize: 14, color: '#9aa0a6', lineHeight: 14 },

  // FAB
  fab: { position: 'absolute', right: 16, bottom: 24 },
  fabCircle: {
    width: 64, height: 64, borderRadius: 32, backgroundColor: '#FFFFFF',
    alignItems: 'center', justifyContent: 'center',
    shadowColor: '#000', shadowOpacity: 0.15, shadowOffset: { width: 0, height: 4 }, shadowRadius: 8, elevation: 6,
  },
});

export default React.memo(HomeScreen);
