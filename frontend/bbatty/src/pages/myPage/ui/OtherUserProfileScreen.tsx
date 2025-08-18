// pages/mypage/OtherUserProfileScreen.tsx
import React, { useEffect } from 'react';
import { View, Text, ScrollView, FlatList, RefreshControl, ActivityIndicator, TouchableOpacity, StatusBar, BackHandler } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation, useRoute, RouteProp, useFocusEffect } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';

import { MyPageStackParamList, RootStackParamList } from '../../../navigation/types';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { useTabBar } from '../../../shared/contexts/TabBarContext';

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

type OtherUserProfileScreenNavigationProp = StackNavigationProp<MyPageStackParamList, 'OtherProfile'>;
type OtherUserProfileScreenRouteProp = RouteProp<MyPageStackParamList, 'OtherProfile'>;

export default function OtherUserProfileScreen() {
  // nav & ui
  const navigation = useNavigation<OtherUserProfileScreenNavigationProp>();
  const route = useRoute<OtherUserProfileScreenRouteProp>();
  const rootNav = useNavigation<StackNavigationProp<RootStackParamList>>();
  const insets = useSafeAreaInsets();
  const themeColor = useThemeColor();
  const { setTabBarVisible } = useTabBar();

  // 대상 유저 파라미터 (항상 다른 사람)
  const targetUserId = route.params.userId; // 필수값
  const fromChatRoom = route.params?.fromChatRoom;
  const isOwner = false; // 항상 다른 사람
  const apiUserId = targetUserId; // 항상 targetUserId 사용


  // 탭바 가시성 제어 (항상 숨김)
  useFocusEffect(
    React.useCallback(() => {
      setTabBarVisible(false);

      return () => {
        setTabBarVisible(true);
      };
    }, [setTabBarVisible])
  );

  // 안드로이드 하드웨어 뒤로가기 처리
  useFocusEffect(
    React.useCallback(() => {
      const onBackPress = () => {
        if (fromChatRoom) {
          rootNav.navigate('MainTabs', {
            screen: 'ChatStack'
          });
          return true;
        }
        
        return false;
      };

      const subscription = BackHandler.addEventListener('hardwareBackPress', onBackPress);
      return () => subscription.remove();
    }, [fromChatRoom, rootNav])
  );

  // 스토어 상태(탭/시즌/승률보기)
  const { activeTab, selectedSeason, winRateType, setActiveTab, setSelectedSeason, setWinRateType } = useProfileStore();
  
  // 다른 사람 프로필 진입 시 탭 상태 리셋
  useEffect(() => {
    setActiveTab('posts');
    setSelectedSeason('total');
    setWinRateType('summary');
  }, [setSelectedSeason, setActiveTab, setWinRateType]);

  // 프로필/통계 쿼리
  const {
    data: profile,
    isLoading: profileLoading,
    error: profileError,
    refetch: refetchProfile,
  } = useProfile(apiUserId);


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
  } = useAllUserStats(apiUserId, selectedSeason);

  const canViewContent = (type: 'posts' | 'stats' | 'attendanceRecords'): boolean => {
    if (isOwner || !profile) return true;
    
    // 공개범위 설정 로그 출력
    console.log(`[공개범위 확인] 사용자 ${targetUserId}의 공개범위:`, {
      postsPublic: profile.postsPublic,
      statsPublic: profile.statsPublic,
      attendanceRecordsPublic: profile.attendanceRecordsPublic,
      requestedType: type
    });
    
    switch (type) {
      case 'posts':
        return profile.postsPublic !== false; // null/undefined는 기본 공개
      case 'stats':
        return profile.statsPublic !== false; // null/undefined는 기본 공개
      case 'attendanceRecords':
        return profile.attendanceRecordsPublic !== false; // null/undefined는 기본 공개
      default:
        return true;
    }
  };

  // 게시글 (다른 사람의 공개된 게시글)
  const meId = useUserStore((s) => s.currentUser?.userId);
  const userIdForPosts = canViewContent('posts') ? apiUserId : undefined;
  const myPostsQ = useMyPostsInfinite(userIdForPosts);
  const myPosts = (myPostsQ.data?.pages ?? []).flatMap((p) => p?.posts ?? []);

  const goPostDetail = (postId: number) => {
    // MyPageStack 내에서 PostDetail로 이동
    navigation.navigate('PostDetail', { postId, fromProfile: true });
  };

  const handleRefreshAll = async () => {
    await Promise.all([refetchProfile(), refetchAllStats()]);
  };

  const handleBackPress = () => {
    if (fromChatRoom) {
      rootNav.navigate('MainTabs', {
        screen: 'ChatStack'
      });
    } else {
      navigation.goBack();
    }
  };

  // 나머지는 UserProfileScreen과 동일한 렌더링 로직
  const renderWinRateBody = () => {
    if (statsLoading) {
      return (
        <View style={styles.loadingContent}>
          <ActivityIndicator size='large' color={themeColor} />
          <Text>통계를 불러오는 중...</Text>
        </View>
      );
    }

    switch (winRateType) {
      case 'summary':
        return (
          <ScrollView style={styles.tabContent} showsVerticalScrollIndicator={false}>
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
          <ScrollView style={styles.tabContent} showsVerticalScrollIndicator={false}>
            {opponentStats.data?.opponentStats && (
              <TeamStatsChart
                teamStats={Object.entries(opponentStats.data.opponentStats).map(([team, stats]: [string, any]) => ({
                  teamId: Number(team),
                  teamName: team,
                  matches: stats.games || 0,
                  wins: stats.wins || 0,
                  draws: stats.draws || 0,
                  losses: stats.losses || 0,
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
          <ScrollView style={styles.tabContent} showsVerticalScrollIndicator={false}>
            {stadiumStats.data?.stadiumStats && (
              <StadiumMapStats
                stadiumStats={stadiumStats.data.stadiumStats}
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
          <ScrollView style={styles.tabContent} showsVerticalScrollIndicator={false}>
            {dayOfWeekStats.data?.dayOfWeekStats && (
              <DetailedStatsGrid
                dayStats={Object.entries(dayOfWeekStats.data.dayOfWeekStats).map(([day, stats]: [string, any]) => ({
                  dayName: day,
                  matches: stats.games || 0,
                  wins: stats.wins || 0,
                  draws: stats.draws || 0,
                  losses: stats.losses || 0,
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
        if (!canViewContent('posts')) {
          return (
            <View style={styles.restrictedContent}>
              <Text style={styles.restrictedText}>작성글 조회가 허용되지 않습니다</Text>
            </View>
          );
        }

        if (myPostsQ.isLoading) {
          return (
            <View style={styles.loadingContent}>
              <ActivityIndicator size='large' color={themeColor} />
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
              myPostsQ.isFetchingNextPage ? <ActivityIndicator style={{ marginVertical: 12 }} /> : <View />
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
              <Text style={styles.restrictedText}>통계 조회가 허용되지 않습니다</Text>
            </View>
          );
        }

        if (statsLoading) {
          return (
            <View style={styles.loadingContent}>
              <ActivityIndicator size='large' color={themeColor} />
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
              userId={apiUserId}
            />
            <ScrollView style={[styles.tabContent, styles.statsTabContent]} showsVerticalScrollIndicator={false}>
              {badges.data && <UserBadgesCard badgeCategories={badges.data.badgeCategories} />}
            </ScrollView>
          </View>
        );
      }

      case 'winrate': {
        if (!canViewContent('stats')) {
          return (
            <View style={styles.restrictedContent}>
              <Text style={styles.restrictedText}>통계 조회가 허용되지 않습니다</Text>
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
              userId={apiUserId}
            />
            <View style={styles.winRateFiltersContainer}>
              <WinRateTypeSelector selectedType={winRateType} onTypeChange={setWinRateType} />
            </View>
            {renderWinRateBody()}
          </View>
        );
      }

      case 'history': {
        if (!canViewContent('attendanceRecords')) {
          return (
            <View style={styles.restrictedContent}>
              <Text style={styles.restrictedText}>직관 기록 조회가 허용되지 않습니다</Text>
            </View>
          );
        }
        return null;
      }

      default:
        return null;
    }
  };

  if (profileLoading) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <ActivityIndicator size='large' color={themeColor} />
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
    <View style={{ flex: 1, backgroundColor: '#fff' }}>
      <StatusBar backgroundColor={themeColor} barStyle="light-content" />
      <View style={{ backgroundColor: themeColor, paddingTop: insets.top }}>
        <UserProfileHeader
          profile={profile}
          basicStats={basicStats.data}
          isOwner={isOwner}
          onBackPress={handleBackPress}
          onSettingsPress={() => {}}
        />
      </View>

      <ProfileTabs activeTab={activeTab} onTabChange={setActiveTab} />

      {activeTab === 'history' && canViewContent('attendanceRecords') && (
        <View style={styles.dropdownContainer}>
          <SeasonSelector selectedSeason={selectedSeason} onSeasonChange={setSelectedSeason} userId={apiUserId} />
        </View>
      )}

      {activeTab === 'history' && canViewContent('attendanceRecords') ? (
        <AttendanceHistory
          userId={apiUserId}
          season={selectedSeason}
          onRecordPress={(r) => {
            // 필요 시 상세 이동
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