import React, { useState, useEffect } from 'react';
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
import { gameApi } from '../../entities/game/api/api';
import type { MatchChatRoom } from '../../entities/chat-room/api/types';
import type { Game } from '../../entities/game/api/types';
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
  const [gameInfoMap, setGameInfoMap] = useState<Map<string, Game>>(new Map());
  const themeColor = useThemeColor();
  const insets = useSafeAreaInsets();

  const getTeamInfo = (teamId: string | number) => {
    const teamInfoMap: { [key: string]: { name: string; color: string } } = {
      // 숫자 ID로 매핑 (teamTypes.ts 기준)
      '1': { name: '한화', color: '#FF6600' },
      '2': { name: 'LG', color: '#C30452' },
      '3': { name: '롯데', color: '#002955' },
      '4': { name: 'KT', color: '#000000' },
      '5': { name: '삼성', color: '#0066B3' },
      '6': { name: 'KIA', color: '#EA0029' },
      '7': { name: 'SSG', color: '#CE0E2D' },
      '8': { name: 'NC', color: '#1D467F' },
      '9': { name: '두산', color: '#131230' },
      '10': { name: '키움', color: '#820024' },
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

  const loadGameInfo = async (gameId: string) => {
    try {
      if (gameInfoMap.has(gameId)) {
        return gameInfoMap.get(gameId);
      }
      
      const response = await gameApi.getGameById(gameId);
      if (response.status === 'SUCCESS') {
        const newGameInfoMap = new Map(gameInfoMap);
        newGameInfoMap.set(gameId, response.data);
        setGameInfoMap(newGameInfoMap);
        return response.data;
      }
    } catch (error) {
      console.error(`게임 정보 로드 실패 (gameId: ${gameId}):`, error);
    }
    return null;
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

  const formatGameDateTime = (dateTimeString: string) => {
    const date = new Date(dateTimeString);
    return date.toLocaleString('ko-KR', {
      month: 'numeric',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const renderRoomItem = ({ item }: { item: MatchChatRoom }) => {
    const teamInfo = getTeamInfo(item.teamId);
    const gameInfo = item.gameId ? gameInfoMap.get(item.gameId) : null;

    // 게임 정보가 없으면 로드 시도 (hook 없이)
    if (item.gameId && !gameInfoMap.has(item.gameId)) {
      loadGameInfo(item.gameId);
    }
    
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
          
          
          {/* 헤더 영역 - 팀 배지만 */}
          <View style={styles.simpleHeader}>
            <View style={[styles.teamBadge, { backgroundColor: teamInfo.color }]}>
              <Text style={styles.teamText}>{teamInfo.name}</Text>
            </View>
          </View>

          {/* 메인 컨텐츠 영역 */}
          <View style={styles.centeredContent}>
            <Text style={styles.roomTitle}>{item.matchTitle}</Text>
            
            {gameInfo && (
              <View style={styles.gameInfoMain}>
                <Text style={styles.gameTeamsText}>
                  {gameInfo.awayTeamName} vs {gameInfo.homeTeamName}
                </Text>
                <Text style={styles.gameDetailsText}>
                  {formatGameDateTime(gameInfo.dateTime)} | {gameInfo.stadium}
                </Text>
              </View>
            )}
            
            <Text style={styles.roomDescription} numberOfLines={2}>
              {item.matchDescription}
            </Text>
          </View>
        </View>

        {/* 하단 정보 영역 */}
        <View style={styles.compactBottomInfo}>
          <Text style={styles.ageGenderInfo}>
            {item.minAge}-{item.maxAge}세 • {item.genderCondition === 'ALL' ? '전체' : 
             item.genderCondition === 'MALE' ? '남성' : '여성'}
          </Text>
          <Text style={styles.timeInfo}>
            {formatDate(item.createdAt)}
          </Text>
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