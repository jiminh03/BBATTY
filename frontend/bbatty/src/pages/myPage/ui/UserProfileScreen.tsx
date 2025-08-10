import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, Image, RefreshControl } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { useQuery } from '@tanstack/react-query';

import { MyPageStackParamList } from '../../../navigation/types';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { useUserStore } from '../../../entities/user';
import { profileApi } from '../../../features/user-profile';
import { useUserStats, useDirectViewHistory } from '../../../features/user-stats/hooks/useUserStats';
import { Season } from '../../../features/user-stats/model/statsTypes';
import { isOk } from '../../../shared/utils/result';

// Components
import { SeasonSelector } from '../../../features/user-profile/ui/SeasonSelector';
import { UserPostsPlaceholder } from '../../../features/post-search/ui/UserPostsPlaceholder';
import { UserBadgesCard } from './UserBadgesCard';
import { WinRateSummaryCard } from './stats/WinRateSummaryCard';
import { TeamStatsChart } from './stats/TeamStatsChart';
import { StadiumMapStats } from './stats/StadiumMapStats';
import { DailyStatsGrid } from './stats/DailyStatsGrid';
import { AttendanceHistory } from './AttendanceHistory';

import { styles } from './UserProfileScreen.style';

type UserProfileScreenNavigationProp = StackNavigationProp<MyPageStackParamList, 'Profile'>;
type UserProfileScreenRouteProp = RouteProp<MyPageStackParamList, 'Profile'>;

interface UserProfileScreenProps {
  navigation: UserProfileScreenNavigationProp;
  route: UserProfileScreenRouteProp;
}

type TabType = 'posts' | 'stats' | 'history';

export default function UserProfileScreen({ navigation, route }: UserProfileScreenProps) {
  const insets = useSafeAreaInsets();
  const themeColor = useThemeColor();
  const { currentUser } = useUserStore();

  // 파라미터에서 userId 가져오기 (없으면 내 프로필)
  const targetUserId = route.params?.userId || currentUser?.userId;
  const isOwner = !route.params?.userId || route.params?.userId === currentUser?.userId;

  const [activeTab, setActiveTab] = useState<TabType>('posts');
  const [selectedSeason, setSelectedSeason] = useState<Season>('전체');

  // 사용자 프로필 정보 조회
  const {
    data: userProfile,
    isLoading: profileLoading,
    refetch: refetchProfile,
  } = useQuery({
    queryKey: ['userProfile', targetUserId],
    queryFn: async () => {
      const result = await profileApi.getProfile(targetUserId);
      if (isOk(result)) {
        return result.data;
      }
      throw new Error(result.error.message);
    },
    enabled: !!targetUserId,
  });

  // 통계 데이터 조회
  const { stats, isLoading: statsLoading, refetch: refetchStats } = useUserStats(targetUserId!, selectedSeason);

  // 직관 기록 조회
  const {
    records,
    isLoading: recordsLoading,
    refetch: refetchRecords,
  } = useDirectViewHistory(targetUserId!, selectedSeason);

  const handleRefresh = async () => {
    await Promise.all([refetchProfile(), refetchStats(), refetchRecords()]);
  };

  const handleSeasonChange = (season: Season) => {
    setSelectedSeason(season);
  };

  const handleEditProfile = () => {
    navigation.navigate('ProfileEdit');
  };

  const handleSettings = () => {
    navigation.navigate('Settings');
  };

  const handleRecordPress = (record: any) => {
    // 경기 상세 모달 또는 페이지로 이동
    console.log('Record pressed:', record);
  };

  const canViewContent = (type: 'posts' | 'stats' | 'history') => {
    if (isOwner) return true;

    if (!userProfile?.privacySettings) return true;

    switch (type) {
      case 'posts':
        return userProfile.privacySettings.allowViewPosts;
      case 'stats':
        return userProfile.privacySettings.allowViewStats;
      case 'history':
        return userProfile.privacySettings.allowViewDirectViewHistory;
      default:
        return true;
    }
  };

  const renderTabContent = () => {
    switch (activeTab) {
      case 'posts':
        return <UserPostsPlaceholder userId={targetUserId!} canViewPosts={canViewContent('posts')} />;

      case 'stats':
        if (!canViewContent('stats')) {
          return (
            <View style={styles.restrictedContent}>
              <Text style={styles.restrictedText}>이 사용자는 통계 조회를 허용하지 않습니다</Text>
            </View>
          );
        }

        if (statsLoading) {
          return (
            <View style={styles.loadingContent}>
              <Text>통계를 불러오는 중...</Text>
            </View>
          );
        }

        if (!stats) {
          return (
            <View style={styles.emptyContent}>
              <Text>통계 데이터가 없습니다</Text>
            </View>
          );
        }

        return (
          <ScrollView style={styles.tabContent} showsVerticalScrollIndicator={false}>
            <UserBadgesCard badges={stats.badges} />
            <WinRateSummaryCard winRates={stats.winRates} />
            <TeamStatsChart teamStats={stats.teamStats} />
            <StadiumMapStats stadiumStats={stats.stadiumStats} />
            <DailyStatsGrid dayStats={stats.dayStats} totalStats={stats.winRates} />
          </ScrollView>
        );

      case 'history':
        if (!canViewContent('history')) {
          return (
            <View style={styles.restrictedContent}>
              <Text style={styles.restrictedText}>이 사용자는 직관 기록 조회를 허용하지 않습니다</Text>
            </View>
          );
        }

        return <AttendanceHistory records={records || []} onRecordPress={handleRecordPress} />;

      default:
        return null;
    }
  };

  if (profileLoading) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <Text>프로필을 불러오는 중...</Text>
      </View>
    );
  }

  if (!userProfile) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <Text>사용자 정보를 찾을 수 없습니다</Text>
      </View>
    );
  }

  const tabs = [
    { key: 'posts' as TabType, label: '작성글' },
    { key: 'stats' as TabType, label: '통계' },
    { key: 'history' as TabType, label: '직관기록' },
  ];

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={[styles.header, { backgroundColor: themeColor }]}>
        <View style={styles.headerContent}>
          {!isOwner && (
            <TouchableOpacity style={styles.backButton} onPress={() => navigation.goBack()}>
              <Ionicons name='chevron-back' size={24} color='#FFFFFF' />
            </TouchableOpacity>
          )}

          <View style={styles.profileSection}>
            <View style={styles.profileImageContainer}>
              {userProfile.profileImg ? (
                <Image source={{ uri: userProfile.profileImg }} style={styles.profileImage} />
              ) : (
                <View style={styles.profileImagePlaceholder}>
                  <Ionicons name='person' size={40} color='#CCCCCC' />
                </View>
              )}
            </View>

            <View style={styles.profileInfo}>
              <Text style={styles.nickname}>{userProfile.nickname}</Text>
              <Text style={styles.winRate}>{userProfile.totalWinRate}% 승률</Text>
              {userProfile.introduction && <Text style={styles.introduction}>{userProfile.introduction}</Text>}
            </View>
          </View>

          {isOwner && (
            <TouchableOpacity style={styles.settingsButton} onPress={handleSettings}>
              <Ionicons name='settings-outline' size={24} color='#FFFFFF' />
            </TouchableOpacity>
          )}
        </View>
      </View>

      {/* Tabs */}
      <View style={styles.tabsContainer}>
        {tabs.map((tab) => (
          <TouchableOpacity
            key={tab.key}
            style={[styles.tab, activeTab === tab.key && { borderBottomColor: themeColor }]}
            onPress={() => setActiveTab(tab.key)}
          >
            <Text style={[styles.tabText, activeTab === tab.key && { color: themeColor }]}>{tab.label}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Season Selector (통계, 직관기록 탭에서만 표시) */}
      {(activeTab === 'stats' || activeTab === 'history') && canViewContent(activeTab) && (
        <SeasonSelector selectedSeason={selectedSeason} onSeasonChange={handleSeasonChange} />
      )}

      {/* Content */}
      <View style={styles.contentContainer}>{renderTabContent()}</View>
    </View>
  );
}
