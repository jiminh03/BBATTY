import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Image } from 'react-native';
import { ExploreStackScreenProps } from '../../navigation/types';
import { TEAMS } from '../../shared/team/teamTypes';
import { useThemeColor } from '../../shared/team/ThemeContext';

interface UserRanking {
  rank: number;
  username: string;
  teamName: string;
  totalGames: number;
  wins: number;
  losses: number;
  winRate: number;
  isCurrentUser?: boolean;
}

type Props = ExploreStackScreenProps<'UserRanking'>;

export default function UserRankingScreen({ navigation, route }: Props) {
  const [userRankings, setUserRankings] = useState<UserRanking[]>([]);
  const [selectedTeamFilter, setSelectedTeamFilter] = useState('ALL');
  const [loading, setLoading] = useState(false);
  const themeColor = useThemeColor();

  const fetchUserRankings = () => {
    setLoading(true);
    
    // 목업 데이터
    const mockUserRankings: UserRanking[] = [
      { rank: 1, username: "야구왕김철수", teamName: "LG 트윈스", totalGames: 150, wins: 135, losses: 15, winRate: 0.900 },
      { rank: 2, username: "승부사박영희", teamName: "한화 이글스", totalGames: 120, wins: 102, losses: 18, winRate: 0.850 },
      { rank: 3, username: "홈런왕이민수", teamName: "두산 베어스", totalGames: 200, wins: 168, losses: 32, winRate: 0.840 },
      { rank: 4, username: "야구매니아", teamName: "KIA 타이거즈", totalGames: 180, wins: 147, losses: 33, winRate: 0.817 },
      { rank: 5, username: "스트라이크", teamName: "삼성 라이온즈", totalGames: 160, wins: 128, losses: 32, winRate: 0.800 },
      { rank: 6, username: "홈런볼", teamName: "롯데 자이언츠", totalGames: 140, wins: 110, losses: 30, winRate: 0.786 },
      { rank: 7, username: "야구사랑", teamName: "SSG 랜더스", totalGames: 130, wins: 100, losses: 30, winRate: 0.769 },
      { rank: 8, username: "베이스볼킹", teamName: "KT 위즈", totalGames: 125, wins: 95, losses: 30, winRate: 0.760 },
      { rank: 9, username: "글러브마스터", teamName: "NC 다이노스", totalGames: 110, wins: 82, losses: 28, winRate: 0.745 },
      { rank: 10, username: "배트스윙", teamName: "키움 히어로즈", totalGames: 100, wins: 72, losses: 28, winRate: 0.720 },
      { rank: 45, username: "나야나", teamName: "LG 트윈스", totalGames: 50, wins: 25, losses: 25, winRate: 0.500, isCurrentUser: true }
    ];
    
    const filteredRankings = selectedTeamFilter === 'ALL' 
      ? mockUserRankings 
      : mockUserRankings.filter(user => user.teamName === selectedTeamFilter);
    
    setUserRankings(filteredRankings);
    setLoading(false);
  };

  const getShortTeamName = (teamName: string) => {
    return teamName.split(' ')[0];
  };

  const teamFilters = [
    { id: 'ALL', name: 'ALL', logo: null },
    ...TEAMS.map(team => ({ 
      id: team.name, 
      name: getShortTeamName(team.name), 
      logo: team.imagePath 
    }))
  ];

  useEffect(() => {
    fetchUserRankings();
  }, [selectedTeamFilter]);

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingText}>로딩 중...</Text>
      </View>
    );
  }

  const top3Users = userRankings.filter(user => user.rank <= 3 && !user.isCurrentUser);
  const restUsers = userRankings.filter(user => user.rank > 3 && user.rank <= 10 && !user.isCurrentUser);
  const currentUser = userRankings.find(user => user.isCurrentUser);

  const getPodiumHeight = (rank: number) => {
    switch (rank) {
      case 1: return 120;
      case 2: return 100;
      case 3: return 80;
      default: return 80;
    }
  };

  const getMedalColor = (rank: number) => {
    switch (rank) {
      case 1: return '#FFD700';
      case 2: return '#C0C0C0';
      case 3: return '#CD7F32';
      default: return '#999999';
    }
  };

  return (
    <View style={styles.userRankingContainer}>
        {/* 팀 필터 */}
        <View style={styles.teamFilterContainer}>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.teamFilterScroll}>
            {teamFilters.map((filter) => (
              <TouchableOpacity
                key={filter.id}
                style={[
                  styles.teamFilterItem,
                  selectedTeamFilter === filter.id && {
                    ...styles.selectedTeamFilter,
                    borderColor: themeColor,
                  }
                ]}
                onPress={() => setSelectedTeamFilter(filter.id)}
              >
                {filter.logo ? (
                  <Image source={typeof filter.logo === 'string' ? { uri: filter.logo } : filter.logo} style={styles.teamFilterLogo} resizeMode="contain" />
                ) : (
                  <View style={styles.allFilterContainer}>
                    <Text style={styles.allFilterText}>ALL</Text>
                  </View>
                )}
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>

        <ScrollView style={styles.userRankingList}>
          {/* 상위 3명 시상대 */}
          {top3Users.length > 0 && (
            <View style={styles.podiumContainer}>
              <View style={styles.podiumWrapper}>
                {/* 2위 */}
                {top3Users.find(u => u.rank === 2) && (
                  <View style={styles.podiumPosition}>
                    <View style={styles.userProfile}>
                      <View style={[styles.medal, { backgroundColor: getMedalColor(2) }]}>
                        <Text style={styles.medalText}>2</Text>
                      </View>
                      <Text style={styles.podiumUsername} numberOfLines={1}>
                        {top3Users.find(u => u.rank === 2)?.username}
                      </Text>
                      <Text style={styles.podiumTeam}>
                        {getShortTeamName(top3Users.find(u => u.rank === 2)?.teamName || '')}
                      </Text>
                      <Text style={styles.podiumWinRate}>
                        {((top3Users.find(u => u.rank === 2)?.winRate || 0) * 100).toFixed(1)}%
                      </Text>
                    </View>
                    <View style={[styles.podium, { height: getPodiumHeight(2), backgroundColor: getMedalColor(2) }]}>
                      <Text style={styles.podiumRank}>2</Text>
                    </View>
                  </View>
                )}
                
                {/* 1위 (중앙) */}
                {top3Users.find(u => u.rank === 1) && (
                  <View style={styles.podiumPosition}>
                    <View style={styles.userProfile}>
                      <View style={[styles.medal, { backgroundColor: getMedalColor(1) }]}>
                        <Text style={styles.medalText}>1</Text>
                      </View>
                      <Text style={styles.podiumUsername} numberOfLines={1}>
                        {top3Users.find(u => u.rank === 1)?.username}
                      </Text>
                      <Text style={styles.podiumTeam}>
                        {getShortTeamName(top3Users.find(u => u.rank === 1)?.teamName || '')}
                      </Text>
                      <Text style={styles.podiumWinRate}>
                        {((top3Users.find(u => u.rank === 1)?.winRate || 0) * 100).toFixed(1)}%
                      </Text>
                    </View>
                    <View style={[styles.podium, { height: getPodiumHeight(1), backgroundColor: getMedalColor(1) }]}>
                      <Text style={styles.podiumRank}>1</Text>
                    </View>
                  </View>
                )}

                {/* 3위 */}
                {top3Users.find(u => u.rank === 3) && (
                  <View style={styles.podiumPosition}>
                    <View style={styles.userProfile}>
                      <View style={[styles.medal, { backgroundColor: getMedalColor(3) }]}>
                        <Text style={styles.medalText}>3</Text>
                      </View>
                      <Text style={styles.podiumUsername} numberOfLines={1}>
                        {top3Users.find(u => u.rank === 3)?.username}
                      </Text>
                      <Text style={styles.podiumTeam}>
                        {getShortTeamName(top3Users.find(u => u.rank === 3)?.teamName || '')}
                      </Text>
                      <Text style={styles.podiumWinRate}>
                        {((top3Users.find(u => u.rank === 3)?.winRate || 0) * 100).toFixed(1)}%
                      </Text>
                    </View>
                    <View style={[styles.podium, { height: getPodiumHeight(3), backgroundColor: getMedalColor(3) }]}>
                      <Text style={styles.podiumRank}>3</Text>
                    </View>
                  </View>
                )}
              </View>
            </View>
          )}

          {/* 4-10위 일반 리스트 */}
          {restUsers.map((user) => (
            <View key={user.rank} style={styles.userRankingItem}>
              <View style={styles.userRankInfo}>
                <Text style={styles.userRank}>{user.rank}</Text>
                <View style={styles.userDetails}>
                  <Text style={styles.username}>{user.username}</Text>
                  <Text style={styles.userTeam}>{getShortTeamName(user.teamName)}</Text>
                </View>
              </View>
              <View style={styles.userStats}>
                <Text style={styles.userGames}>{user.totalGames}경기</Text>
                <Text style={styles.userWinRate}>{(user.winRate * 100).toFixed(1)}%</Text>
              </View>
            </View>
          ))}
          
          {/* 내 정보 (10위 밖인 경우) */}
          {currentUser && currentUser.rank > 10 && (
            <>
              <View style={styles.divider}>
                <Text style={styles.dividerText}>...</Text>
              </View>
              <View style={[styles.userRankingItem, styles.currentUserItem]}>
                <View style={styles.userRankInfo}>
                  <Text style={styles.userRank}>{currentUser.rank}</Text>
                  <View style={styles.userDetails}>
                    <Text style={styles.username}>{currentUser.username}</Text>
                    <Text style={styles.userTeam}>{getShortTeamName(currentUser.teamName)}</Text>
                  </View>
                </View>
                <View style={styles.userStats}>
                  <Text style={styles.userGames}>{currentUser.totalGames}경기</Text>
                  <Text style={styles.userWinRate}>{(currentUser.winRate * 100).toFixed(1)}%</Text>
                </View>
              </View>
            </>
          )}
        </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 40,
  },
  loadingText: {
    fontSize: 16,
    color: '#666666',
  },
  userRankingContainer: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  teamFilterContainer: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    backgroundColor: '#ffffff',
  },
  teamFilterScroll: {
    flexDirection: 'row',
  },
  teamFilterItem: {
    alignItems: 'center',
    marginRight: 16,
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 20,
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e0e0e0',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 3,
    },
    shadowOpacity: 0.15,
    shadowRadius: 8,
    elevation: 6,
  },
  selectedTeamFilter: {
    borderWidth: 2,
    shadowOpacity: 0.25,
    shadowRadius: 10,
    elevation: 10,
  },
  teamFilterLogo: {
    width: 64,
    height: 64,
  },
  allFilterContainer: {
    width: 64,
    height: 64,
    justifyContent: 'center',
    alignItems: 'center',
  },
  allFilterText: {
    fontSize: 24,
    color: '#333333',
    fontWeight: 'bold',
  },
  teamFilterName: {
    fontSize: 10,
    color: '#666666',
    fontWeight: '500',
  },
  selectedTeamFilterName: {
    color: '#ffffff',
  },
  userRankingList: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 20,
  },
  userRankingItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    backgroundColor: '#ffffff',
    borderRadius: 8,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#f1f3f4',
  },
  currentUserItem: {
    backgroundColor: '#fff8e1',
    borderColor: '#ffb300',
  },
  userRankInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  userRank: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333333',
    width: 30,
  },
  userDetails: {
    marginLeft: 12,
  },
  username: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333333',
    marginBottom: 2,
  },
  userTeam: {
    fontSize: 12,
    color: '#666666',
  },
  userStats: {
    alignItems: 'flex-end',
  },
  userGames: {
    fontSize: 12,
    color: '#666666',
    marginBottom: 2,
  },
  userWinRate: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#DC143C',
  },
  divider: {
    alignItems: 'center',
    paddingVertical: 12,
  },
  dividerText: {
    fontSize: 18,
    color: '#cccccc',
    fontWeight: 'bold',
  },
  podiumContainer: {
    paddingVertical: 20,
    paddingHorizontal: 16,
    backgroundColor: '#f8f9fa',
    marginBottom: 16,
    borderRadius: 12,
    marginHorizontal: 16,
  },
  podiumWrapper: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'flex-end',
    height: 200,
  },
  podiumPosition: {
    alignItems: 'center',
    flex: 1,
    marginHorizontal: 4,
  },
  userProfile: {
    alignItems: 'center',
    marginBottom: 8,
    minHeight: 100,
  },
  medal: {
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 8,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  medalText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  podiumUsername: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 2,
    textAlign: 'center',
    maxWidth: 80,
  },
  podiumTeam: {
    fontSize: 10,
    color: '#666666',
    marginBottom: 4,
    textAlign: 'center',
  },
  podiumWinRate: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#DC143C',
    textAlign: 'center',
  },
  podium: {
    width: '100%',
    maxWidth: 80,
    justifyContent: 'center',
    alignItems: 'center',
    borderTopLeftRadius: 8,
    borderTopRightRadius: 8,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  podiumRank: {
    color: '#ffffff',
    fontSize: 24,
    fontWeight: 'bold',
  },
});