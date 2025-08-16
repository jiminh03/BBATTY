// pages/explore/CommunityHomeScreen.tsx
import React, { useMemo, useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ExploreStackScreenProps } from '../../navigation/types';
import { useThemeColor } from '../../shared/team/ThemeContext';
import TeamRankingContent from './TeamRankingScreen';
import UserRankingContent from './UserRankingScreen';
import TeamCommunityContent from './TeamCommunityScreen';

type Props = ExploreStackScreenProps<'CommunityHome'>;

const TABS = [
  { id: 'teamranking', label: '팀순위' },
  { id: 'userranking', label: '유저 랭킹' },
  { id: 'teamcommunity', label: '타팀 커뮤니티' },
] as const;

const HEADER_HEIGHT = 56;
const COMMUNITY_TAB_STORAGE_KEY = 'communityHome_selectedTab';

export default function CommunityHomeScreen({ navigation, route}: Props) {
  const [active, setActive] =
    useState<(typeof TABS)[number]['id']>('teamranking');
  const themeColor = useThemeColor();
  const insets = useSafeAreaInsets();

  // 선택된 탭 상태를 로컬 스토리지에서 불러오기
  useEffect(() => {
    const loadSelectedTab = async () => {
      try {
        const savedTab = await AsyncStorage.getItem(COMMUNITY_TAB_STORAGE_KEY);
        if (savedTab && TABS.some(tab => tab.id === savedTab)) {
          setActive(savedTab as (typeof TABS)[number]['id']);
        }
      } catch (error) {
        console.error('선택된 탭 로드 실패:', error);
      }
    };
    loadSelectedTab();
  }, []);

  // 탭 변경 시 로컬 스토리지에 저장
  const handleTabChange = async (tabId: (typeof TABS)[number]['id']) => {
    try {
      setActive(tabId);
      await AsyncStorage.setItem(COMMUNITY_TAB_STORAGE_KEY, tabId);
    } catch (error) {
      console.error('선택된 탭 저장 실패:', error);
    }
  };

  const content = useMemo(() => {
  switch (active) {
    case 'teamranking':
      // 자식이 ExploreStackScreenProps<'TeamRanking'>을 요구하므로 캐스팅해서 전달
      return (
        <TeamRankingContent
          navigation={navigation as any}
          route={route as any}
        />
      );
    case 'userranking':
      return (
        <UserRankingContent
          navigation={navigation as any}
          route={route as any}
        />
      );
    case 'teamcommunity':
      return (
        <TeamCommunityContent
          navigation={navigation as any}
          route={route as any}
        />
      );
    default:
      return null;
  }
}, [active, navigation, route]);

  return (
    <View style={styles.container}>
      {/* 헤더 (안전영역 포함) */}
      <View
        style={[
          styles.header,
          {
            paddingTop: insets.top,
            height: HEADER_HEIGHT + insets.top,
            backgroundColor: themeColor,
          },
        ]}
      >
        <Text style={styles.headerTitle}>탐색</Text>
      </View>

      {/* 상단 탭 - 중앙정렬 */}
      <View style={styles.tabsContainer}>
        <View style={styles.tabsRow}>
          {TABS.map((t) => {
            const isActive = active === t.id;
            return (
              <TouchableOpacity
                key={t.id}
                style={[
                  styles.tab,
                  isActive && { borderBottomColor: themeColor },
                ]}
                onPress={() => handleTabChange(t.id)}
                activeOpacity={0.8}
              >
                <Text
                  style={[styles.tabText, isActive && { color: themeColor }]}
                >
                  {t.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>
      </View>

      {/* 컨텐츠 */}
      <View style={styles.content}>{content}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },

  header: {
    paddingHorizontal: 16,
    justifyContent: 'center',
  },
  headerTitle: { fontSize: 20, fontWeight: 'bold', color: '#fff' },

  tabsContainer: {
    backgroundColor: '#fff',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E0E0E0',
  },
  tabsRow: {
    flexDirection: 'row',
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  tabText: { 
    fontSize: 14, 
    fontWeight: '600', 
    color: '#666',
    textAlign: 'center',
  },

  content: { flex: 1, backgroundColor: '#fff' },
});
