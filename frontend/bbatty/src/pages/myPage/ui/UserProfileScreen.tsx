import React, { useState } from 'react';
import { View, Text, ScrollView, RefreshControl, ActivityIndicator, Alert, TouchableOpacity } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';

import { MyPageStackParamList } from '../../../navigation/types';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { useProfile, useAllUserStats } from '../../../features/user-profile';
import { Season } from '../../../shared';

// Components
import { SeasonSelector } from '../../../features/user-profile/ui/SeasonSelector';
import { UserProfileHeader } from '../../../features/user-profile';
import { ProfileTabs } from '../../../features/user-profile';
import { UserBadgesCard } from './UserBadgesCard';
import { WinRateSummaryCard } from './stats/WinRateSummaryCard';
import { DetailedStatsGrid } from './stats/DailyStatsGrid';

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

  const [activeTab, setActiveTab] = useState<TabType>('posts');
  const [selectedSeason, setSelectedSeason] = useState<Season>('total');

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

  const renderTabContent = () => {
    switch (activeTab) {
      case 'posts':
        return canViewContent('posts') ? (
          <View /*style={styles.placeholderContent}*/>
            <Text /*style={styles.placeholderText}*/>게시글 컴포넌트 자리</Text>
          </View>
        ) : (
          <View style={styles.restrictedContent}>
            <Text style={styles.restrictedText}>게시글 조회가 허용되지 않습니다</Text>
          </View>
        );

      case 'stats':
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
              <Text /*style={styles.loadingText}*/>통계를 불러오는 중...</Text>
            </View>
          );
        }

        return (
          <ScrollView style={styles.tabContent} showsVerticalScrollIndicator={false}>
            {badges.data && <UserBadgesCard badgeCategories={badges.data.badgeCategories} />}
            {basicStats.data && <WinRateSummaryCard basicStats={basicStats.data} homeAwayStats={homeAwayStats.data} />}
            {dayOfWeekStats.data?.dayOfWeekStats && (
              <DetailedStatsGrid
                dayStats={Object.entries(dayOfWeekStats.data.dayOfWeekStats).map(([day, stats]: [string, any]) => ({
                  dayName: day,
                  matches: stats.totalGames || 0,
                  wins: stats.wins || 0,
                  winRate: Math.round((stats.winRate || 0) * 100),
                }))}
              />
            )}
          </ScrollView>
        );

      case 'history':
        return canViewContent('attendanceRecords') ? (
          <AttendanceHistory records={[]} onRecordPress={() => {}}></AttendanceHistory>
        ) : (
          <View style={styles.restrictedContent}>
            <Text style={styles.restrictedText}>직관 기록 조회가 허용되지 않습니다</Text>
          </View>
        );

      default:
        return null;
    }
  };

  if (profileLoading) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <ActivityIndicator size='large' color={themeColor} />
        <Text /*style={styles.loadingText}*/>프로필을 불러오는 중...</Text>
      </View>
    );
  }

  if (profileError || !profile) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <Text /*style={styles.errorText}*/>사용자 정보를 찾을 수 없습니다</Text>
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

      {(activeTab === 'stats' || activeTab === 'history') &&
        canViewContent(activeTab === 'stats' ? 'stats' : 'attendanceRecords') && (
          <SeasonSelector selectedSeason={selectedSeason} onSeasonChange={setSelectedSeason} />
        )}

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
    </View>
  );
}
