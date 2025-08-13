import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ExploreStackScreenProps } from '../../navigation/types';
import { useThemeColor } from '../../shared/team/ThemeContext';
import TeamRankingContent from './TeamRankingScreen';
import UserRankingContent from './UserRankingScreen';
import TeamCommunityContent from './TeamCommunityScreen';

type Props = ExploreStackScreenProps<'CommunityHome'>;

const tabs = [
  { id: 'teamranking', name: '팀순위' },
  { id: 'userranking', name: '유저 랭킹' },
  { id: 'teamcommunity', name: '타팀 커뮤니티' }
];

export default function CommunityHomeScreen({ navigation, route }: Props) {
  const [activeTab, setActiveTab] = useState('teamranking');
  const themeColor = useThemeColor();
  const insets = useSafeAreaInsets();

  const renderTabContent = () => {
    switch (activeTab) {
      case 'teamranking':
        return <TeamRankingContent navigation={navigation} route={route} />;
      case 'userranking':
        return <UserRankingContent navigation={navigation} route={route} />;
      case 'teamcommunity':
        return <TeamCommunityContent navigation={navigation} route={route} />;
      default:
        return null;
    }
  };

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={[styles.header, { backgroundColor: themeColor }]}>
        <Text style={styles.headerTitle}>탐색</Text>
      </View>
      
      <View style={styles.tabsContainer}>
        {tabs.map((tab) => (
          <TouchableOpacity
            key={tab.id}
            style={[
              styles.tab, 
              activeTab === tab.id && { 
                borderBottomColor: themeColor 
              }
            ]}
            onPress={() => setActiveTab(tab.id)}
          >
            <Text style={[
              styles.tabText, 
              activeTab === tab.id && { 
                color: themeColor 
              }
            ]}>
              {tab.name}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
      
      <View style={styles.content}>
        {renderTabContent()}
      </View>
    </View>
  );
}

// 공통 헤더 높이 상수
const HEADER_HEIGHT = 56;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  header: {
    paddingHorizontal: 16,
    height: HEADER_HEIGHT,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#ffffff',
  },
  tabsContainer: {
    flexDirection: 'row',
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  tab: {
    flex: 1,
    paddingVertical: 16,
    alignItems: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  activeTab: {
    borderBottomWidth: 2,
  },
  tabText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#666666',
  },
  activeTabText: {
  },
  content: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
});