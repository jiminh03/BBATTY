import React from 'react';
import { View, Text, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity } from 'react-native';
import { ExploreStackScreenProps } from '../../navigation/types';

type Props = ExploreStackScreenProps<'CommunityHome'>;

export default function CommunityHomeScreen({ navigation, route }: Props) {
  const teams = [
    { id: 'lg', name: 'LG 트윈스', color: '#C30452' },
    { id: 'doosan', name: '두산 베어스', color: '#131230' },
    { id: 'kia', name: 'KIA 타이거즈', color: '#EA0029' },
    { id: 'samsung', name: '삼성 라이온즈', color: '#074CA1' },
    { id: 'lotte', name: '롯데 자이언츠', color: '#041E42' },
    { id: 'ssg', name: 'SSG 랜더스', color: '#CE0E2D' },
    { id: 'kt', name: 'KT 위즈', color: '#000000' },
    { id: 'hanwha', name: '한화 이글스', color: '#FF6600' },
    { id: 'nc', name: 'NC 다이노스', color: '#315288' },
    { id: 'kiwoom', name: '키움 히어로즈', color: '#570514' },
  ];

  const categories = [
    { id: 'all', name: '전체', icon: '⚾' },
    { id: 'game', name: '경기 토론', icon: '🏟️' },
    { id: 'news', name: '야구 뉴스', icon: '📰' },
    { id: 'analysis', name: '경기 분석', icon: '📊' },
    { id: 'free', name: '자유 게시판', icon: '💬' },
  ];

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>탐색</Text>
        <Text style={styles.headerSubtitle}>팀별 커뮤니티와 게시글을 탐색해보세요</Text>
      </View>
      
      <ScrollView style={styles.content}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>📂 카테고리</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.categoryContainer}>
            {categories.map((category) => (
              <TouchableOpacity key={category.id} style={styles.categoryCard}>
                <Text style={styles.categoryIcon}>{category.icon}</Text>
                <Text style={styles.categoryName}>{category.name}</Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>⚾ 팀별 커뮤니티</Text>
          <View style={styles.teamsGrid}>
            {teams.map((team) => (
              <TouchableOpacity 
                key={team.id} 
                style={[styles.teamCard, { borderLeftColor: team.color }]}
              >
                <Text style={styles.teamName}>{team.name}</Text>
                <Text style={styles.teamInfo}>게시글 125개</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🔥 인기 게시글</Text>
          <View style={styles.postCard}>
            <View style={styles.postHeader}>
              <Text style={styles.postCategory}>경기 토론</Text>
              <Text style={styles.postTime}>2시간 전</Text>
            </View>
            <Text style={styles.postTitle}>오늘 LG vs 두산 경기 예상</Text>
            <Text style={styles.postContent}>날씨도 좋고 양 팀 상태도 괜찮아서 재미있는 경기가 될 것 같네요...</Text>
            <View style={styles.postStats}>
              <Text style={styles.postStat}>👍 24</Text>
              <Text style={styles.postStat}>💬 12</Text>
              <Text style={styles.postStat}>👁️ 145</Text>
            </View>
          </View>
          
          <View style={styles.postCard}>
            <View style={styles.postHeader}>
              <Text style={styles.postCategory}>야구 뉴스</Text>
              <Text style={styles.postTime}>4시간 전</Text>
            </View>
            <Text style={styles.postTitle}>신인 선수 활약상 정리</Text>
            <Text style={styles.postContent}>이번 시즌 신인 선수들의 활약이 정말 눈부시네요...</Text>
            <View style={styles.postStats}>
              <Text style={styles.postStat}>👍 18</Text>
              <Text style={styles.postStat}>💬 7</Text>
              <Text style={styles.postStat}>👁️ 89</Text>
            </View>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#4CAF50',
    paddingHorizontal: 20,
    paddingVertical: 20,
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 4,
  },
  headerSubtitle: {
    fontSize: 16,
    color: 'rgba(255, 255, 255, 0.8)',
    textAlign: 'center',
  },
  content: {
    flex: 1,
    padding: 16,
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  categoryContainer: {
    marginTop: 8,
  },
  categoryCard: {
    backgroundColor: '#fff',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 20,
    marginRight: 12,
    alignItems: 'center',
    minWidth: 80,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
  },
  categoryIcon: {
    fontSize: 20,
    marginBottom: 4,
  },
  categoryName: {
    fontSize: 12,
    fontWeight: '600',
    color: '#333',
  },
  teamsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  teamCard: {
    backgroundColor: '#fff',
    padding: 12,
    borderRadius: 8,
    marginBottom: 12,
    width: '48%',
    borderLeftWidth: 4,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
  },
  teamName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  teamInfo: {
    fontSize: 12,
    color: '#666',
  },
  postCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  postHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  postCategory: {
    fontSize: 12,
    fontWeight: '600',
    color: '#4CAF50',
    backgroundColor: 'rgba(76, 175, 80, 0.1)',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  postTime: {
    fontSize: 12,
    color: '#999',
  },
  postTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  postContent: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
    marginBottom: 12,
  },
  postStats: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
  },
  postStat: {
    fontSize: 12,
    color: '#999',
  },
});