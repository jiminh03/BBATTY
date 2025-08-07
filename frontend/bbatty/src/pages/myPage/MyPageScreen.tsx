import React from 'react';
import { View, Text, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity, Image } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { MyPageStackScreenProps } from '../../navigation/types';

type Props = MyPageStackScreenProps<'MyPage'>;

export default function MyPageScreen({ navigation, route }: Props) {
  const menuItems = [
    { id: 'profile', title: '프로필 수정', icon: 'person-outline', onPress: () => {} },
    { id: 'attendance', title: '출석 히스토리', icon: 'calendar-outline', onPress: () => {} },
    { id: 'settings', title: '설정', icon: 'settings-outline', onPress: () => {} },
    { id: 'notifications', title: '알림 설정', icon: 'notifications-outline', onPress: () => {} },
    { id: 'blocked', title: '차단된 사용자', icon: 'ban-outline', onPress: () => {} },
    { id: 'about', title: '앱 정보', icon: 'information-circle-outline', onPress: () => {} },
  ];

  const stats = [
    { label: '작성한 글', value: '12' },
    { label: '댓글', value: '48' },
    { label: '좋아요', value: '156' },
  ];

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>마이페이지</Text>
      </View>
      
      <ScrollView style={styles.content}>
        {/* 프로필 섹션 */}
        <View style={styles.profileSection}>
          <View style={styles.profileImageContainer}>
            <View style={styles.profileImage}>
              <Ionicons name="person" size={40} color="#999" />
            </View>
          </View>
          <View style={styles.profileInfo}>
            <Text style={styles.nickname}>야구팬123</Text>
            <Text style={styles.email}>baseball@example.com</Text>
            <View style={styles.teamBadge}>
              <Text style={styles.teamBadgeText}>LG 트윈스 팬</Text>
            </View>
          </View>
        </View>

        {/* 통계 섹션 */}
        <View style={styles.statsSection}>
          {stats.map((stat, index) => (
            <View key={stat.label} style={styles.statItem}>
              <Text style={styles.statValue}>{stat.value}</Text>
              <Text style={styles.statLabel}>{stat.label}</Text>
            </View>
          ))}
        </View>

        {/* 메뉴 섹션 */}
        <View style={styles.menuSection}>
          {menuItems.map((item, index) => (
            <TouchableOpacity key={item.id} style={styles.menuItem} onPress={item.onPress}>
              <View style={styles.menuItemLeft}>
                <Ionicons name={item.icon as any} size={20} color="#666" />
                <Text style={styles.menuItemTitle}>{item.title}</Text>
              </View>
              <Ionicons name="chevron-forward" size={16} color="#ccc" />
            </TouchableOpacity>
          ))}
        </View>

        {/* 로그아웃 버튼 */}
        <View style={styles.logoutSection}>
          <TouchableOpacity style={styles.logoutButton}>
            <Text style={styles.logoutButtonText}>로그아웃</Text>
          </TouchableOpacity>
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
    backgroundColor: '#FF6B35',
    paddingHorizontal: 20,
    paddingVertical: 20,
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#fff',
  },
  content: {
    flex: 1,
  },
  profileSection: {
    backgroundColor: '#fff',
    padding: 20,
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
  },
  profileImageContainer: {
    marginRight: 16,
  },
  profileImage: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#f0f0f0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  profileInfo: {
    flex: 1,
  },
  nickname: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  email: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  teamBadge: {
    backgroundColor: '#C30452',
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 12,
    alignSelf: 'flex-start',
  },
  teamBadgeText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#fff',
  },
  statsSection: {
    backgroundColor: '#fff',
    padding: 20,
    marginBottom: 12,
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  statItem: {
    alignItems: 'center',
  },
  statValue: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#FF6B35',
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 14,
    color: '#666',
  },
  menuSection: {
    backgroundColor: '#fff',
    marginBottom: 12,
  },
  menuItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  menuItemLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  menuItemTitle: {
    fontSize: 16,
    color: '#333',
    marginLeft: 12,
  },
  logoutSection: {
    padding: 20,
  },
  logoutButton: {
    backgroundColor: '#FF4444',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  logoutButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});