import React from 'react';
import { View, Text, StyleSheet, SafeAreaView, ScrollView, TouchableOpacity, Alert } from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import { MaterialIcons } from '@expo/vector-icons';
import { useUserStore } from '../../entities/user/model/userStore';

type Props = HomeStackScreenProps<'Home'>;

export default function HomeScreen({ navigation, route }: Props) {
  const teamId = useUserStore((s) => s.currentUser?.teamId);   // ✅ 하드코딩 제거

  const goPostList = () => {
    if (!teamId) {
      Alert.alert('팀 선택 필요', '내 팀 정보가 없습니다. 로그인/팀 선택을 먼저 완료해주세요.');
      return;
    }
    navigation.navigate('PostList', { teamId });
  };
  
  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>BBATTY</Text>
        <Text style={styles.headerSubtitle}>야구 팬들의 소통 공간</Text>
      
      <TouchableOpacity
          style={styles.penButton}
          onPress={() => navigation.navigate('PostForm')}
        >
          <MaterialIcons name="edit" size={24} color="#fff" />
        </TouchableOpacity>
      
      </View>
      
      <ScrollView style={styles.content}>
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🏟️ 오늘의 경기</Text>
          <View style={styles.gameCard}>
            <Text style={styles.gameText}>LG 트윈스 vs 두산 베어스</Text>
            <Text style={styles.gameTime}>19:30 | 잠실야구장</Text>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🔥 인기 게시글</Text>
          <View style={styles.postCard}>
            <Text style={styles.postTitle}>오늘 경기 어떻게 보셨나요?</Text>
            <Text style={styles.postInfo}>댓글 24 • 좋아요 15</Text>
          </View>
          <View style={styles.postCard}>
            <Text style={styles.postTitle}>시즌 전망 어떻게 생각하시나요?</Text>
            <Text style={styles.postInfo}>댓글 18 • 좋아요 8</Text>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>💬 빠른 기능</Text>
          <TouchableOpacity 
            style={styles.quickButton}
            onPress={() => {
              // 직관 인증 페이지로 이동
              navigation.navigate('AttendanceVerification' as never);
            }}
          >
            <Text style={styles.quickButtonText}>🎯 직관 인증하기</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.quickButton, !teamId && { opacity: 0.5 }]}
            disabled={!teamId}
            onPress={goPostList}
          >
            <Text style={styles.quickButtonText}>게시글 조회</Text>
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
    backgroundColor: '#007AFF',
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
  gameCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  gameText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  gameTime: {
    fontSize: 14,
    color: '#666',
  },
  postCard: {
    backgroundColor: '#fff',
    padding: 12,
    borderRadius: 8,
    marginBottom: 8,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
  },
  postTitle: {
    fontSize: 14,
    fontWeight: '500',
    color: '#333',
    marginBottom: 4,
  },
  postInfo: {
    fontSize: 12,
    color: '#999',
  },
  quickButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  quickButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
   penButton: {
    position: 'absolute',
    right: 20,
    top: 20,
    backgroundColor: '#005BBB',
    padding: 8,
    borderRadius: 20,
  },
});
