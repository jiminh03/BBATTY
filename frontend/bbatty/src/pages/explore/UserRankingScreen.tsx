import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Image } from 'react-native';
import { ExploreStackScreenProps } from '../../navigation/types';
import { TEAMS } from '../../shared/team/teamTypes';

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

  const top10Users = userRankings.filter(user => user.rank <= 10 && !user.isCurrentUser);
  const currentUser = userRankings.find(user => user.isCurrentUser);

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
                  selectedTeamFilter === filter.id && styles.selectedTeamFilter
                ]}
                onPress={() => setSelectedTeamFilter(filter.id)}
              >
                {filter.logo ? (
                  <Image source={{ uri: filter.logo }} style={styles.teamFilterLogo} />
                ) : (
                  <View style={styles.allFilterIcon}>
                    <Text style={styles.allFilterText}>ALL</Text>
                  </View>
                )}
                <Text style={[
                  styles.teamFilterName,
                  selectedTeamFilter === filter.id && styles.selectedTeamFilterName
                ]}>
                  {filter.name}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>

        {/* 유저 랭킹 리스트 */}
        <ScrollView style={styles.userRankingList}>
          {top10Users.map((user) => (
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
    paddingVertical: 16,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f1f3f4',
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
    backgroundColor: '#f5f5f5',
  },
  selectedTeamFilter: {
    backgroundColor: '#DC143C',
  },
  teamFilterLogo: {
    width: 20,
    height: 20,
    marginBottom: 4,
  },
  allFilterIcon: {
    width: 20,
    height: 20,
    backgroundColor: '#666666',
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 4,
  },
  allFilterText: {
    fontSize: 8,
    color: '#ffffff',
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
});