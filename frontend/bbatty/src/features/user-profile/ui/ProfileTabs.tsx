import React from 'react';
import { View, TouchableOpacity, Text } from 'react-native';
import { useThemeColor } from '../../../shared/team/ThemeContext';
import { styles } from './ProfileTabs.style';

export type TabType = 'posts' | 'stats' | 'history';

interface ProfileTabsProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
}

const tabs = [
  { key: 'posts' as TabType, label: '작성글' },
  { key: 'stats' as TabType, label: '통계' },
  { key: 'history' as TabType, label: '직관기록' },
];

export const ProfileTabs: React.FC<ProfileTabsProps> = ({ activeTab, onTabChange }) => {
  const themeColor = useThemeColor();

  return (
    <View style={styles.tabsContainer}>
      {tabs.map((tab) => (
        <TouchableOpacity
          key={tab.key}
          style={[styles.tab, activeTab === tab.key && { borderBottomColor: themeColor }]}
          onPress={() => onTabChange(tab.key)}
        >
          <Text style={[styles.tabText, activeTab === tab.key && { color: themeColor }]}>{tab.label}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );
};
