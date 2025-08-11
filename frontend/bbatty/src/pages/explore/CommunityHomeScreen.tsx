import React, { useState } from 'react';
import { View, Text, StyleSheet, SafeAreaView, TouchableOpacity } from 'react-native';
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
    <SafeAreaView style={styles.container}>
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
                ...styles.activeTab, 
                borderColor: themeColor 
              }
            ]}
            onPress={() => setActiveTab(tab.id)}
          >
            <Text style={[
              styles.tabText, 
              activeTab === tab.id && { 
                ...styles.activeTabText, 
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
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  header: {
    paddingHorizontal: 16,
    paddingVertical: 16,
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#ffffff',
  },
  tabsContainer: {
    flexDirection: 'row',
    backgroundColor: '#ffffff',
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 16,
  },
  tab: {
    flex: 1,
    paddingVertical: 12,
    paddingHorizontal: 8,
    marginHorizontal: 4,
    borderRadius: 8,
    backgroundColor: '#ffffff',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e0e0e0',
  },
  activeTab: {
    backgroundColor: '#ffffff',
    borderWidth: 2,
  },
  tabText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333333',
  },
  activeTabText: {
  },
  content: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
});