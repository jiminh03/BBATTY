import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  Alert,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { chatRoomApi } from '../../entities/chat-room/api/api';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { ChatStackParamList } from '../../navigation/types';
import { useThemeColor } from '../../shared/team/ThemeContext';
import { styles } from './MatchChatRoomListScreen.styles';

type NavigationProp = StackNavigationProp<ChatStackParamList>;

export default function ChatRoomSearchScreen() {
  const navigation = useNavigation<NavigationProp>();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState<MatchChatRoom[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const themeColor = useThemeColor();
  const insets = useSafeAreaInsets();

  const getTeamInfo = (teamId: string | number) => {
    const teamInfoMap: { [key: string]: { name: string; color: string } } = {
      // 숫자 ID로 매핑
      '1': { name: 'KIA', color: '#EA0029' },
      '2': { name: '삼성', color: '#074CA1' },
      '3': { name: 'LG', color: '#C30452' },
      '4': { name: '두산', color: '#131230' },
      '5': { name: 'KT', color: '#000000' },
      '6': { name: 'SSG', color: '#CE0E2D' },
      '7': { name: '롯데', color: '#041E42' },
      '8': { name: '한화', color: '#FF6600' },
      '9': { name: 'NC', color: '#315288' },
      '10': { name: '키움', color: '#570514' },
      // 문자열 ID도 지원 (기존 호환성)
      'LG': { name: 'LG', color: '#C30452' },
      '두산': { name: '두산', color: '#131230' },
      'KIA': { name: 'KIA', color: '#EA0029' },
      '삼성': { name: '삼성', color: '#074CA1' },
      '롯데': { name: '롯데', color: '#041E42' },
      'SSG': { name: 'SSG', color: '#CE0E2D' },
      'KT': { name: 'KT', color: '#000000' },
      '한화': { name: '한화', color: '#FF6600' },
      'NC': { name: 'NC', color: '#315288' },
      '키움': { name: '키움', color: '#570514' },
    };
    const key = String(teamId);
    return teamInfoMap[key] || { name: `팀 ${teamId}`, color: '#007AFF' };
  };

  const executeSearch = async () => {
    if (!searchKeyword.trim()) {
      Alert.alert('알림', '검색어를 입력해주세요.');
      return;
    }

    try {
      setLoading(true);
      const response = await chatRoomApi.getMatchChatRooms(searchKeyword.trim());
      
      if (response.data?.data?.chatRooms) {
        setSearchResults(response.data.data.chatRooms);
      } else if (response.data?.data?.rooms) {
        setSearchResults(response.data.data.rooms);
      } else if (response.data?.rooms) {
        setSearchResults(response.data.rooms);
      } else {
        setSearchResults([]);
      }
      setHasSearched(true);
    } catch (error) {
      console.error('채팅방 검색 실패:', error);
      Alert.alert('오류', '채팅방 검색에 실패했습니다.');
      setSearchResults([]);
      setHasSearched(true);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (minutes < 60) return `${minutes}분 전`;
    if (hours < 24) return `${hours}시간 전`;
    return `${days}일 전`;
  };

  const renderRoomItem = ({ item }: { item: MatchChatRoom }) => {
    const teamInfo = getTeamInfo(item.teamId);
    
    return (
      <TouchableOpacity
        style={styles.roomItem}
        onPress={() => navigation.navigate('MatchChatRoomDetail', { room: item })}
        activeOpacity={0.9}
      >
        <View style={styles.topSection}>
          <LinearGradient
            colors={['#049fbb', '#50f6ff']}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.gradientBackground}
          />
          
          {/* 장식적 테두리 요소 */}
          <View style={styles.borderElement} />
          
          {/* 헤더 영역 */}
          <View style={styles.roomHeader}>
            <View style={styles.logoContainer}>
              <View style={[styles.teamBadge, { backgroundColor: teamInfo.color }]}>
                <Text style={styles.teamText}>{teamInfo.name}</Text>
              </View>
            </View>
            <View style={styles.socialMediaContainer}>
              <Text style={[styles.statusText, { color: '#ffffff' }]}>
                {item.status === 'ACTIVE' ? '모집중' : '마감'}
              </Text>
            </View>
          </View>

          {/* 제목 영역 */}
          <View style={styles.titleContainer}>
            <Text style={styles.roomTitle}>{item.matchTitle}</Text>
            <Text style={styles.roomDescription} numberOfLines={2}>
              {item.matchDescription}
            </Text>
          </View>
        </View>

        <View style={styles.bottomContent}>
          <View style={styles.roomInfo}>
            <View style={styles.infoItem}>
              <Text style={styles.infoLabel}>연령</Text>
              <Text style={styles.infoValue}>{item.minAge}-{item.maxAge}세</Text>
            </View>
            <View style={styles.infoItem}>
              <Text style={styles.infoLabel}>참여자</Text>
              <Text style={[styles.infoValue, styles.participantCount]}>
                {item.currentParticipants}/{item.maxParticipants}
              </Text>
            </View>
            <View style={styles.infoItem}>
              <Text style={styles.infoLabel}>성별</Text>
              <Text style={styles.infoValue}>
                {item.genderCondition === 'ALL' ? '전체' : 
                 item.genderCondition === 'MALE' ? '남성' : '여성'}
              </Text>
            </View>
          </View>
          
          <View style={styles.roomFooter}>
            <Text style={styles.createdAt}>
              {formatDate(item.createdAt)}
            </Text>
            <View style={[
              styles.statusBadge, 
              item.status === 'ACTIVE' ? styles.activeBadge : styles.inactiveBadge
            ]}>
              <Text style={styles.statusText}>
                {item.status === 'ACTIVE' ? '모집중' : '마감'}
              </Text>
            </View>
          </View>
        </View>
      </TouchableOpacity>
    );
  };

  const EmptyComponent = () => (
    <View style={styles.emptyContainer}>
      <View style={styles.emptyIconContainer}>
        <Text style={styles.emptyIcon}>🔍</Text>
      </View>
      {hasSearched ? (
        <>
          <Text style={styles.emptyText}>검색 결과가 없습니다</Text>
          <Text style={styles.emptySubtext}>다른 검색어로 다시 시도해보세요</Text>
        </>
      ) : (
        <>
          <Text style={styles.emptyText}>채팅방을 검색해보세요</Text>
          <Text style={styles.emptySubtext}>채팅방 제목으로 검색할 수 있습니다</Text>
        </>
      )}
    </View>
  );

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={[styles.header, { backgroundColor: themeColor }]}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => navigation.goBack()}
        >
          <Text style={styles.backButtonText}>←</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>채팅방 검색</Text>
        <View style={styles.placeholder} />
      </View>

      <View style={styles.searchContainer}>
        <View style={styles.searchInputContainer}>
          <TextInput
            style={styles.searchInput}
            placeholder="채팅방 제목, 설명, 팀명으로 검색..."
            placeholderTextColor="#999"
            value={searchKeyword}
            onChangeText={setSearchKeyword}
            autoFocus={true}
            returnKeyType="search"
            onSubmitEditing={executeSearch}
          />
          <TouchableOpacity
            style={[styles.searchButtonInline, { backgroundColor: themeColor }]}
            onPress={executeSearch}
            disabled={loading}
            activeOpacity={0.8}
          >
            <Text style={styles.searchButtonInlineText}>
              {loading ? '검색중...' : '검색'}
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      <FlatList
        data={searchResults}
        renderItem={renderRoomItem}
        keyExtractor={(item) => item.matchId}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={EmptyComponent}
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
}