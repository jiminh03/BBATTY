// pages/explore/CommunityHomeScreen.tsx
import React, { useMemo, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
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

export default function CommunityHomeScreen({ navigation, route}: Props) {
  const [active, setActive] =
    useState<(typeof TABS)[number]['id']>('teamranking');
  const themeColor = useThemeColor();
  const insets = useSafeAreaInsets();

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

      {/* 상단 탭 */}
      <View style={styles.tabs}>
        {TABS.map((t) => {
          const isActive = active === t.id;
          return (
            <TouchableOpacity
              key={t.id}
              style={[
                styles.tab,
                isActive && { borderBottomColor: themeColor },
              ]}
              onPress={() => setActive(t.id)}
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

  tabs: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E0E0E0',
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 14,
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  tabText: { fontSize: 16, fontWeight: '600', color: '#666' },

  content: { flex: 1, backgroundColor: '#fff' },
});
