// pages/mypage/UserProfileScreen.tsx
import React from 'react';
import {
  View,
  Text,
  ScrollView,
  FlatList,
  RefreshControl,
  ActivityIndicator,
  TouchableOpacity,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';

import {
  MyPageStackParamList,
  RootStackParamList,
} from '../../../navigation/types';
import { useThemeColor } from '../../../shared/team/ThemeContext';

import { useProfile, useAllUserStats } from '../../../features/user-profile';
import { useProfileStore } from '../../../features/user-profile/model/profileStore';

import { SeasonSelector } from '../../../features/user-profile/ui/SeasonSelector';
import { UserProfileHeader } from '../../../features/user-profile';
import { ProfileTabs } from '../../../features/user-profile';
import { StatsTabHeader } from '../../../features/user-profile';
import { WinRateTypeSelector } from '../../../features/user-profile';
import { UserBadgesCard } from './UserBadgesCard';
import { WinRateSummaryCard } from './stats/WinRateSummaryCard';
import { DetailedStatsGrid } from './stats/DailyStatsGrid';
import { StadiumMapStats } from './stats/StadiumMapStats';
import { TeamStatsChart } from './stats/TeamStatsChart';

import { styles } from './UserProfileScreen.style';
import { AttendanceHistory } from './AttendanceHistory';

import { useMyPostsInfinite } from '../../../entities/post/queries';
import { PostItem } from '../../../entities/post/ui/PostItem';
import { useUserStore } from '../../../entities/user/model/userStore';

type UserProfileScreenNavigationProp = StackNavigationProp<
  MyPageStackParamList,
  'Profile'
>;
type UserProfileScreenRouteProp = RouteProp<MyPageStackParamList, 'Profile'>;

export default function UserProfileScreen() {
  // nav & ui
  const navigation = useNavigation<UserProfileScreenNavigationProp>();
  const route = useRoute<UserProfileScreenRouteProp>();
  const rootNav = useNavigation<StackNavigationProp<RootStackParamList>>();
  const insets = useSafeAreaInsets();
  const themeColor = useThemeColor();

  // 대상 유저 파라미터
  const targetUserId = route.params?.userId;
  const isOwner = !targetUserId;

  // 스토어 상태(탭/시즌/승률보기)
  const {
    activeTab,
    selectedSeason,
    winRateType,
    setActiveTab,
    setSelectedSeason,
    setWinRateType,
  } = useProfileStore();

  // 프로필/통계 쿼리
  const {
    data: profile,
    isLoading: profileLoading,
    error: profileError,
    refetch: refetchProfile,
  } = useProfile(targetUserId);

  const {
    basicStats,
    homeAwayStats,
    dayOfWeekStats,
    stadiumStats,
    opponentStats,
    streakStats,
    badges,
    isLoading: statsLoading,
    refetchAll: refetchAllStats,
  } = useAllUserStats(targetUserId, selectedSeason);

  // 내 게시글 (오너일 때만)
  const meId = useUserStore((s) => s.currentUser?.userId);
  const myUserIdForPosts = isOwner ? (profile?.userId ?? meId) : undefined;
  const myPostsQ = useMyPostsInfinite(myUserIdForPosts);
  const myPosts = (myPostsQ.data?.pages ?? []).flatMap((p) => p?.posts ?? []);

  const goPostDetail = (postId: number) => {
    // 루트 네비를 통해 HomeStack의 PostDetail로 이동
    rootNav.navigate('MainTabs', {
      screen: 'HomeStack',
      params: { screen: 'PostDetail', params: { postId } },
    });
  };

  const handleRefreshAll = async () => {
    await Promise.all([refetchProfile(), refetchAllStats()]);
  };

  const canViewContent = (
    type: 'posts' | 'stats' | 'attendanceRecords',
  ): boolean => {
    if (isOwner || !profile) return true;
    switch (type) {
      case 'posts':
        return profile.postsPublic;
      case 'stats':
        return profile.statsPublic;
      case 'attendanceRecords':
        return profile.attendanceRecordsPublic;
      default:
        return true;
    }
  };

  const renderWinRateBody = () => {
    if (statsLoading) {
      return (
        <View style={styles.loadingContent}>
          <ActivityIndicator size="large" color={themeColor} />
          <Text>통계를 불러오는 중...</Text>
        </View>
      );
    }

    switch (winRateType) {
      case 'summary':
        return (
          <ScrollView
            style={styles.tabContent}
            showsVerticalScrollIndicator={false}
          >
            {basicStats.data && (
              <WinRateSummaryCard
                basicStats={basicStats.data}
                homeAwayStats={homeAwayStats.data}
                streakStats={streakStats.data}
              />
            )}
          </ScrollView>
        );
      case 'opponent':
        return (
          <ScrollView
            style={styles.tabContent}
            showsVerticalScrollIndicator={false}
          >
            {opponentStats.data?.opponentStats && (
              <TeamStatsChart
                teamStats={Object.entries(
                  opponentStats.data.opponentStats,
                ).map(([team, stats]: [string, any]) => ({
                  teamId: Number(team),
                  teamName: team,
                  matches: stats.games || 0,
                  wins: stats.wins || 0,
                  winRate: Math.round((parseFloat(stats.winRate) || 0) * 100),
                }))}
                totalGames={basicStats.data?.totalGames || 0}
                winRate={basicStats.data?.winRate || '0'}
                wins={basicStats.data?.wins || 0}
                draws={basicStats.data?.draws || 0}
                losses={basicStats.data?.losses || 0}
                streakStats={streakStats.data}
              />
            )}
          </ScrollView>
        );
      case 'stadium':
        return (
          <ScrollView
            style={styles.tabContent}
            showsVerticalScrollIndicator={false}
          >
            {stadiumStats.data?.stadiumStats && (
              <StadiumMapStats
                stadiumStats={Object.entries(
                  stadiumStats.data.stadiumStats,
                ).map(([stadium, stats]: [string, any]) => ({
                  stadiumId: Number(stadium),
                  stadiumName: stadium,
                  matches: stats.games || 0,
                  wins: stats.wins || 0,
                  winRate: Math.round((parseFloat(stats.winRate) || 0) * 100),
                }))}
                totalGames={basicStats.data?.totalGames || 0}
                winRate={basicStats.data?.winRate || '0'}
                wins={basicStats.data?.wins || 0}
                draws={basicStats.data?.draws || 0}
                losses={basicStats.data?.losses || 0}
                streakStats={streakStats.data}
              />
            )}
          </ScrollView>
        );
      case 'dayOfWeek':
        return (
          <ScrollView
            style={styles.tabContent}
            showsVerticalScrollIndicator={false}
          >
            {dayOfWeekStats.data?.dayOfWeekStats && (
              <DetailedStatsGrid
                dayStats={Object.entries(
                  dayOfWeekStats.data.dayOfWeekStats,
                ).map(([day, stats]: [string, any]) => ({
                  dayName: day,
                  matches: stats.games || 0,
                  wins: stats.wins || 0,
                  winRate: Math.round((parseFloat(stats.winRate) || 0) * 100),
                }))}
                totalGames={basicStats.data?.totalGames || 0}
                winRate={basicStats.data?.winRate || '0'}
                wins={basicStats.data?.wins || 0}
                draws={basicStats.data?.draws || 0}
                losses={basicStats.data?.losses || 0}
                streakStats={streakStats.data}
              />
            )}
          </ScrollView>
        );
      default:
        return null;
    }
  };

  const renderTabContent = () => {
    switch (activeTab) {
      case 'posts': {
        // 내 게시글만 허용
        if (!isOwner) {
          return (
            <View style={styles.restrictedContent}>
              <Text style={styles.restrictedText}>내 게시글만 볼 수 있어요</Text>
            </View>
          );
        }

        if (myPostsQ.isLoading) {
          return (
            <View style={styles.loadingContent}>
              <ActivityIndicator size="large" color={themeColor} />
              <Text>게시글을 불러오는 중...</Text>
            </View>
          );
        }

        return (
          <FlatList
            data={myPosts}
            keyExtractor={(i: any) => String(i.id)}
            renderItem={({ item }) => (
              <TouchableOpacity onPress={() => goPostDetail(item.id)}>
                <PostItem post={item} onPress={() => goPostDetail(item.id)} />
              </TouchableOpacity>
            )}
            onEndReachedThreshold={0.35}
            onEndReached={() => {
              if (myPostsQ.hasNextPage && !myPostsQ.isFetchingNextPage) {
                myPostsQ.fetchNextPage();
              }
            }}
            ListFooterComponent={
              myPostsQ.isFetchingNextPage ? (
                <ActivityIndicator style={{ marginVertical: 12 }} />
              ) : (
                <View />
              )
            }
            ListEmptyComponent={
              !myPostsQ.isFetching && myPosts.length === 0 ? (
                <View style={styles.emptyContent}>
                  <Text>작성한 게시글이 없어요</Text>
                </View>
              ) : null
            }
            refreshControl={
              <RefreshControl
                refreshing={myPostsQ.isFetching && !myPostsQ.isFetchingNextPage}
                onRefresh={() => myPostsQ.refetch()}
                tintColor={themeColor}
              />
            }
            contentContainerStyle={{ paddingBottom: 16 }}
          />
        );
      }

      case 'badges': {
        if (!canViewContent('stats')) {
          return (
            <View style={styles.restrictedContent}>
              <Text style={styles.restrictedText}>
                통계 조회가 허용되지 않습니다
              </Text>
            </View>
          );
        }

        if (statsLoading) {
          return (
            <View style={styles.loadingContent}>
              <ActivityIndicator size="large" color={themeColor} />
              <Text>뱃지를 불러오는 중...</Text>
            </View>
          );
        }

        return (
          <View style={styles.statsContentContainer}>
            <StatsTabHeader
              activeTab={activeTab}
              onTabChange={setActiveTab}
              selectedSeason={selectedSeason}
              onSeasonChange={setSelectedSeason}
            />
            <ScrollView
              style={[styles.tabContent, styles.statsTabContent]}
              showsVerticalScrollIndicator={false}
            >
              {badges.data && (
                <UserBadgesCard
                  badgeCategories={badges.data.badgeCategories}
                />
              )}
            </ScrollView>
          </View>
        );
      }

      case 'winrate': {
        if (!canViewContent('stats')) {
          return (
            <View style={styles.restrictedContent}>
              <Text style={styles.restrictedText}>
                통계 조회가 허용되지 않습니다
              </Text>
            </View>
          );
        }

        return (
          <View style={styles.statsContentContainer}>
            <StatsTabHeader
              activeTab={activeTab}
              onTabChange={setActiveTab}
              selectedSeason={selectedSeason}
              onSeasonChange={setSelectedSeason}
            />
            <View style={styles.winRateFiltersContainer}>
              <WinRateTypeSelector
                selectedType={winRateType}
                onTypeChange={setWinRateType}
              />
            </View>
            {renderWinRateBody()}
          </View>
        );
      }

      case 'history': {
        if (!canViewContent('attendanceRecords')) {
          return (
            <View style={styles.restrictedContent}>
              <Text style={styles.restrictedText}>
                직관 기록 조회가 허용되지 않습니다
              </Text>
            </View>
          );
        }
        return null; // AttendanceHistory는 아래에서 별도로 렌더
      }

      default:
        return null;
    }
  };

  if (profileLoading) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <ActivityIndicator size="large" color={themeColor} />
        <Text>프로필을 불러오는 중...</Text>
      </View>
    );
  }

  if (profileError || !profile) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <Text>사용자 정보를 찾을 수 없습니다</Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <UserProfileHeader
        profile={profile}
        basicStats={basicStats.data}
        isOwner={isOwner}
        onBackPress={() => navigation.goBack()}
        onSettingsPress={() => navigation.navigate('Settings')}
      />

      <ProfileTabs activeTab={activeTab} onTabChange={setActiveTab} />

      {/* 직관기록 탭: 시즌 선택기 */}
      {activeTab === 'history' && canViewContent('attendanceRecords') && (
        <View style={styles.dropdownContainer}>
          <SeasonSelector
            selectedSeason={selectedSeason}
            onSeasonChange={setSelectedSeason}
          />
        </View>
      )}

      {/* history 탭은 리스트 컴포넌트 직접, posts 탭은 FlatList 직접, 그 외는 ScrollView */}
      {activeTab === 'history' && canViewContent('attendanceRecords') ? (
        <AttendanceHistory
          userId={targetUserId}
          season={selectedSeason}
          onRecordPress={(r) => {
            // 필요 시 상세 이동
            console.log('직관 기록 클릭:', r);
          }}
        />
      ) : activeTab === 'posts' ? (
        renderTabContent()
      ) : (
        <ScrollView
          style={styles.contentContainer}
          refreshControl={
            <RefreshControl
              refreshing={profileLoading || statsLoading}
              onRefresh={handleRefreshAll}
              tintColor={themeColor}
            />
          }
        >
          {renderTabContent()}
        </ScrollView>
      )}
    </View>
  );
}
