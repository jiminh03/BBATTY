import React from 'react';
import { View, TouchableOpacity, Text } from 'react-native';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { styles } from './ProfileTabs.style';

export type TabType = 'posts' | 'badges' | 'winrate' | 'history';

// 메인 탭을 위한 별도 타입
type MainTabKey = 'posts' | 'stats' | 'history';

interface ProfileTabsProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
}

const mainTabs: { key: MainTabKey; label: string }[] = [
  { key: 'posts', label: '작성글' },
  { key: 'stats', label: '통계' },
  { key: 'history', label: '직관기록' },
];

// Stats subtabs removed - will be handled in StatsTabHeader component

export const ProfileTabs: React.FC<ProfileTabsProps> = ({ activeTab, onTabChange }) => {
  const themeColor = useThemeColor();

  // 통계 관련 탭인지 확인
  const isStatsTab = activeTab === 'badges' || activeTab === 'winrate';

  const handleMainTabPress = (tabKey: MainTabKey) => {
    if (tabKey === 'stats') {
      // 통계 탭 클릭 시 기본적으로 뱃지 탭으로 이동
      onTabChange('badges');
    } else {
      // posts, history는 TabType과 동일하므로 안전하게 변환
      onTabChange(tabKey as TabType);
    }
  };

  return (
    <View style={styles.tabsContainer}>
      {mainTabs.map((tab) => {
        const isActive =
          (tab.key === 'posts' && activeTab === 'posts') ||
          (tab.key === 'history' && activeTab === 'history') ||
          (tab.key === 'stats' && isStatsTab);

        return (
          <TouchableOpacity
            key={tab.key}
            style={[styles.tab, isActive && { borderBottomColor: themeColor }]}
            onPress={() => handleMainTabPress(tab.key)}
          >
            <Text style={[styles.tabText, isActive && { color: themeColor }]}>{tab.label}</Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );
};
