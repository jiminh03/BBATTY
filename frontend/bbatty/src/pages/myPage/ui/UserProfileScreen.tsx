import React, { useState } from 'react';
import { View, Text, ScrollView, RefreshControl, ActivityIndicator, Alert, TouchableOpacity } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';

import { MyPageStackParamList } from '../../../navigation/types';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { useProfile, useAllUserStats } from '../../../features/user-profile';
import { useProfileStore } from '../../../features/user-profile/model/profileStore';
import { Season } from '../../../shared/utils/date';

// Components
import { SeasonSelector } from '../../../features/user-profile/ui/SeasonSelector';
import { UserProfileHeader } from '../../../features/user-profile';
import { ProfileTabs } from '../../../features/user-profile';
import { StatsTabHeader } from '../../../features/user-profile';
import { WinRateTypeSelector, WinRateType } from '../../../features/user-profile';
import { UserBadgesCard } from './UserBadgesCard';
import { WinRateSummaryCard } from './stats/WinRateSummaryCard';
import { DetailedStatsGrid } from './stats/DailyStatsGrid';
import { StadiumMapStats } from './stats/StadiumMapStats';
import { TeamStatsChart } from './stats/TeamStatsChart';

import { styles } from './UserProfileScreen.style';
import { TabType } from '../../../features/user-profile/ui/ProfileTabs';
import { AttendanceHistory } from './AttendanceHistory';

type UserProfileScreenNavigationProp = StackNavigationProp<MyPageStackParamList, 'Profile'>;
type UserProfileScreenRouteProp = RouteProp<MyPageStackParamList, 'Profile'>;

export default function UserProfileScreen() {
  const navigation = useNavigation<UserProfileScreenNavigationProp>();
  const route = useRoute<UserProfileScreenRouteProp>();
  const insets = useSafeAreaInsets();
  const themeColor = useThemeColor();

  const targetUserId = route.params?.userId;
  const isOwner = !targetUserId;

  // Use store for tab state persistence
  const {
    activeTab,
    selectedSeason,
    winRateType,
    setActiveTab,
    setSelectedSeason,
    setWinRateType,
  } = useProfileStore();

  // 프로필 조회
  const {
    data: profile,
    isLoading: profileLoading,
    error: profileError,
    refetch: refetchProfile,
  } = useProfile(targetUserId);

  // 모든 통계 조회
  const {
    basicStats,
    homeAwayStats,
    dayOfWeekStats,
    stadiumStats,
    opponentStats,
    streakStats,
    badges,
    isLoading: statsLoading,
    isError: statsError,
    refetchAll: refetchAllStats,
  } = useAllUserStats(targetUserId, selectedSeason);


  const handleRefresh = async () => {
    try {
      await Promise.all([refetchProfile(), refetchAllStats()]);
    } catch (error) {
      Alert.alert('오류', '데이터를 새로고침하는데 실패했습니다.');
    }
  };

  const canViewContent = (type: 'posts' | 'stats' | 'attendanceRecords') => {
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

  // 승률 타입별 컴포넌트 렌더링
  const renderWinRateContent = () => {
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
            {basicStats.data && <WinRateSummaryCard basicStats={basicStats.data} homeAwayStats={homeAwayStats.data} streakStats={streakStats.data} />}
          </ScrollView>
        );
      case 'opponent':
        return (
          <ScrollView style={styles.tabContent} showsVerticalScrollIndicator={false}>
            {opponentStats.data?.opponentStats && (
              <TeamStatsChart
                teamStats={Object.entries(opponentStats.data.opponentStats).map(([team, stats]: [string, any]) => ({
                  teamId: parseInt(team),
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
          <ScrollView style={styles.tabContent} showsVerticalScrollIndicator={false}>
            {stadiumStats.data?.stadiumStats && (
              <StadiumMapStats
                stadiumStats={Object.entries(stadiumStats.data.stadiumStats).map(([stadium, stats]: [string, any]) => ({
                  stadiumId: parseInt(stadium),
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
          <ScrollView style={styles.tabContent} showsVerticalScrollIndicator={false}>
            {dayOfWeekStats.data?.dayOfWeekStats && (
              <DetailedStatsGrid
                dayStats={Object.entries(dayOfWeekStats.data.dayOfWeekStats).map(([day, stats]: [string, any]) => ({
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
      case 'posts':
        return canViewContent('posts') ? (
          <View style={styles.emptyContent}>
            <Text style={styles.restrictedText}>게시글 컴포넌트 자리</Text>
          </View>
        ) : (
          <View style={styles.restrictedContent}>
            <Text style={styles.restrictedText}>게시글 조회가 허용되지 않습니다</Text>
          </View>
        );

      case 'badges':
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
            />
            <ScrollView style={[styles.tabContent, styles.statsTabContent]} showsVerticalScrollIndicator={false}>
              {badges.data && <UserBadgesCard badgeCategories={badges.data.badgeCategories} />}
            </ScrollView>
          </View>
        );

      case 'winrate':
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
            />
            <View style={styles.winRateFiltersContainer}>
              <WinRateTypeSelector selectedType={winRateType} onTypeChange={setWinRateType} />
            </View>
            {renderWinRateContent()}
          </View>
        );

      case 'history':
        if (!canViewContent('attendanceRecords')) {
          return (
            <View style={styles.restrictedContent}>
              <Text style={styles.restrictedText}>직관 기록 조회가 허용되지 않습니다</Text>
            </View>
          );
        }

        return null; // AttendanceHistory는 ScrollView 밖에서 렌더링

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
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <UserProfileHeader
        profile={profile}
        basicStats={basicStats.data}
        isOwner={isOwner}
        onBackPress={() => navigation.goBack()}
        onSettingsPress={() => navigation.navigate('Settings')}
      />

      <ProfileTabs activeTab={activeTab} onTabChange={setActiveTab} />

      {/* 직관기록 탭 - 시즌 선택기만 표시 */}
      {activeTab === 'history' && canViewContent('attendanceRecords') && (
        <View style={styles.dropdownContainer}>
          <SeasonSelector selectedSeason={selectedSeason} onSeasonChange={setSelectedSeason} />
        </View>
      )}

      {/* AttendanceHistory는 ScrollView 밖에서 렌더링 (VirtualizedList 중첩 방지) */}
      {activeTab === 'history' && canViewContent('attendanceRecords') ? (
        <AttendanceHistory
          userId={targetUserId}
          season={selectedSeason}
          onRecordPress={(record) => {
            console.log('직관 기록 클릭:', record);
          }}
        />
      ) : (
        <ScrollView
          style={styles.contentContainer}
          refreshControl={
            <RefreshControl
              refreshing={profileLoading || statsLoading}
              onRefresh={handleRefresh}
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
