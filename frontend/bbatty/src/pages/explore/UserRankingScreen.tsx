import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Image, Alert } from 'react-native';
import { ExploreStackScreenProps } from '../../navigation/types';
import { TEAMS, findTeamById } from '../../shared/team/teamTypes';
import { useThemeColor } from '../../shared/team/ThemeContext';
import { useUserStore } from '../../entities/user/model/userStore';
import { apiClient } from '../../shared/api/client/apiClient';

interface UserRanking {
  userId: number;
  nickname: string;
  winRate: number;
  rank: number;
  percentile: number | null;
  isCurrentUser: boolean;
  userTeamId?: number;
}

interface RankingResponse {
  season: string;
  rankings: UserRanking[];
  myRanking: UserRanking | null;
}

interface TeamRankingResponse {
  teamId: number;
  teamName: string;
  season: string;
  rankings: UserRanking[];
  myRanking: UserRanking | null;
}

type Props = ExploreStackScreenProps<'UserRanking'>;

export default function UserRankingScreen({ navigation, route }: Props) {
  const [allRankings, setAllRankings] = useState<UserRanking[]>([]);
  const [teamRankingsData, setTeamRankingsData] = useState<{[key: string]: UserRanking[]}>({});
  const [myGlobalRanking, setMyGlobalRanking] = useState<UserRanking | null>(null);
  const [myTeamRankings, setMyTeamRankings] = useState<{[key: string]: UserRanking | null}>({});
  const [selectedTeamFilter, setSelectedTeamFilter] = useState<string>('ALL');
  const [loading, setLoading] = useState(false);
  const [initialDataLoaded, setInitialDataLoaded] = useState(false);
  const themeColor = useThemeColor();
  const getCurrentUser = useUserStore((state) => state.getCurrentUser);
  const currentUser = getCurrentUser();
  const scrollViewRef = useRef<ScrollView>(null);

  const fetchAllRankingsData = async () => {
    if (!currentUser?.userId) {
      Alert.alert('오류', '사용자 정보를 찾을 수 없습니다.');
      return;
    }

    setLoading(true);
    try {
      // 전체 랭킹 조회
      const globalResponse = await apiClient.get<{ status: string; message: string; data: RankingResponse }>(
        `/api/ranking/global`
      );
      
      if (globalResponse.data.status === 'SUCCESS') {
        setAllRankings(globalResponse.data.data.rankings);
        setMyGlobalRanking(globalResponse.data.data.myRanking);
      }

      // 모든 팀별 랭킹 조회
      const teamRankingsPromises = TEAMS.map(async (team) => {
        try {
          const teamResponse = await apiClient.get<{ status: string; message: string; data: TeamRankingResponse }>(
            `/api/ranking/team/${team.id}`
          );
          
          if (teamResponse.data.status === 'SUCCESS') {
            return {
              teamName: team.name,
              rankings: teamResponse.data.data.rankings,
              myRanking: teamResponse.data.data.myRanking
            };
          }
        } catch (error) {
          console.error(`팀 ${team.name} 랭킹 조회 실패:`, error);
        }
        return null;
      });

      const teamResults = await Promise.all(teamRankingsPromises);
      const newTeamRankingsData: {[key: string]: UserRanking[]} = {};
      const newMyTeamRankings: {[key: string]: UserRanking | null} = {};

      teamResults.forEach((result) => {
        if (result) {
          newTeamRankingsData[result.teamName] = result.rankings;
          newMyTeamRankings[result.teamName] = result.myRanking;
        }
      });

      setTeamRankingsData(newTeamRankingsData);
      setMyTeamRankings(newMyTeamRankings);
      setInitialDataLoaded(true);

    } catch (error) {
      console.error('랭킹 조회 실패:', error);
      Alert.alert('오류', '랭킹 정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const getTeamNameByUserId = (userId: number) => {
    // 팀명 표시 안함 (임시)
    return '';
  };

  const teamFilters = [
    { id: 'ALL', name: 'ALL', logo: null },
    ...TEAMS.map(team => ({ 
      id: team.name, 
      name: team.name.split(' ')[0], 
      logo: team.imagePath 
    }))
  ];

  // 현재 선택된 필터에 따른 데이터 가져오기
  const getCurrentRankingData = () => {
    if (selectedTeamFilter === 'ALL') {
      return {
        rankings: allRankings,
        myRanking: myGlobalRanking
      };
    } else {
      return {
        rankings: teamRankingsData[selectedTeamFilter] || [],
        myRanking: myTeamRankings[selectedTeamFilter] || null
      };
    }
  };

  useEffect(() => {
    if (!initialDataLoaded) {
      fetchAllRankingsData();
    }
  }, []);

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingText}>로딩 중...</Text>
      </View>
    );
  }

  const { rankings: currentRankings, myRanking: currentMyRanking } = getCurrentRankingData();
  const top3Users = currentRankings.filter(user => user.rank <= 3 && !user.isCurrentUser);
  const restUsers = currentRankings.filter(user => user.rank > 3 && user.rank <= 10 && !user.isCurrentUser);
  const currentUserRanking = currentRankings.find(user => user.isCurrentUser) || currentMyRanking;

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
          <ScrollView 
            ref={scrollViewRef}
            horizontal 
            showsHorizontalScrollIndicator={false} 
            style={styles.teamFilterScroll}
            maintainVisibleContentPosition={{
              minIndexForVisible: 0,
              autoscrollToTopThreshold: 0,
            }}
          >
            {teamFilters.map((filter, index) => (
              <TouchableOpacity
                key={filter.id}
                style={[
                  styles.teamFilterItem,
                  selectedTeamFilter === filter.id && {
                    ...styles.selectedTeamFilter,
                    backgroundColor: themeColor,
                  }
                ]}
                onPress={() => setSelectedTeamFilter(filter.id)}
              >
                {filter.logo ? (
                  <View style={styles.teamLogoContainer}>
                    <Image 
                      source={typeof filter.logo === 'string' ? { uri: filter.logo } : filter.logo} 
                      style={[
                        styles.teamFilterLogo,
                        selectedTeamFilter === filter.id && styles.selectedTeamLogo
                      ]} 
                      resizeMode="contain" 
                    />
                    <Text style={[
                      styles.teamFilterName,
                      selectedTeamFilter === filter.id && styles.selectedTeamFilterName
                    ]}>
                      {filter.name}
                    </Text>
                  </View>
                ) : (
                  <View style={styles.allFilterContainer}>
                    <Text style={[
                      styles.allFilterText,
                      selectedTeamFilter === filter.id && styles.selectedAllFilterText
                    ]}>ALL</Text>
                  </View>
                )}
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>

        <ScrollView 
          style={styles.userRankingList}
          contentContainerStyle={styles.scrollContent}
        >
          {/* 상위 3명 시상대 */}
          {top3Users.length > 0 && (
            <View style={styles.podiumContainer}>
              <View style={styles.podiumWrapper}>
                {/* 2위 */}
                {(() => {
                  const user = top3Users.find(u => u.rank === 2);
                  const team = findTeamById(user?.userTeamId || 0);
                  return user && (
                    <View style={styles.podiumPosition}>
                      <View style={styles.userProfile}>
                        <View style={[styles.crownContainer, { backgroundColor: getMedalColor(2) }]}>
                          <Text style={styles.crownText}>👑</Text>
                          <Text style={styles.rankNumber}>2</Text>
                        </View>
                        <View style={styles.podiumUserInfo}>
                          {team && (
                            <Image 
                              source={team.imagePath}
                              style={styles.podiumTeamLogo}
                              resizeMode="contain"
                            />
                          )}
                          <Text style={styles.podiumUsername} numberOfLines={1}>
                            {user.nickname}
                          </Text>
                        </View>
                        <Text style={styles.podiumWinRate}>
                          {(user.winRate * 100).toFixed(1)}%
                        </Text>
                      </View>
                      <View style={[styles.podium, { height: getPodiumHeight(2), backgroundColor: getMedalColor(2) }]}>
                        <Text style={styles.podiumRank}>2nd</Text>
                      </View>
                    </View>
                  );
                })()}
                
                {/* 1위 (중앙) */}
                {(() => {
                  const user = top3Users.find(u => u.rank === 1);
                  const team = findTeamById(user?.userTeamId || 0);
                  return user && (
                    <View style={styles.podiumPosition}>
                      <View style={styles.userProfile}>
                        <View style={[styles.crownContainer, { backgroundColor: getMedalColor(1) }]}>
                          <Text style={styles.crownText}>👑</Text>
                          <Text style={styles.rankNumber}>1</Text>
                        </View>
                        <View style={styles.podiumUserInfo}>
                          {team && (
                            <Image 
                              source={team.imagePath}
                              style={styles.podiumTeamLogo}
                              resizeMode="contain"
                            />
                          )}
                          <Text style={styles.podiumUsername} numberOfLines={1}>
                            {user.nickname}
                          </Text>
                        </View>
                        <Text style={styles.podiumWinRate}>
                          {(user.winRate * 100).toFixed(1)}%
                        </Text>
                      </View>
                      <View style={[styles.podium, { height: getPodiumHeight(1), backgroundColor: getMedalColor(1) }]}>
                        <Text style={styles.podiumRank}>1st</Text>
                        <View style={styles.sparkles}>
                          <Text style={styles.sparkle}>✨</Text>
                          <Text style={styles.sparkle}>✨</Text>
                        </View>
                      </View>
                    </View>
                  );
                })()}

                {/* 3위 */}
                {(() => {
                  const user = top3Users.find(u => u.rank === 3);
                  const team = findTeamById(user?.userTeamId || 0);
                  return user && (
                    <View style={styles.podiumPosition}>
                      <View style={styles.userProfile}>
                        <View style={[styles.crownContainer, { backgroundColor: getMedalColor(3) }]}>
                          <Text style={styles.crownText}>👑</Text>
                          <Text style={styles.rankNumber}>3</Text>
                        </View>
                        <View style={styles.podiumUserInfo}>
                          {team && (
                            <Image 
                              source={team.imagePath}
                              style={styles.podiumTeamLogo}
                              resizeMode="contain"
                            />
                          )}
                          <Text style={styles.podiumUsername} numberOfLines={1}>
                            {user.nickname}
                          </Text>
                        </View>
                        <Text style={styles.podiumWinRate}>
                          {(user.winRate * 100).toFixed(1)}%
                        </Text>
                      </View>
                      <View style={[styles.podium, { height: getPodiumHeight(3), backgroundColor: getMedalColor(3) }]}>
                        <Text style={styles.podiumRank}>3rd</Text>
                      </View>
                    </View>
                  );
                })()}
              </View>
            </View>
          )}

          {/* 순위 데이터가 없는 경우 */}
          {currentRankings.length === 0 && !loading && (
            <View style={styles.emptyRankingContainer}>
              <Text style={styles.emptyRankingIcon}>📊</Text>
              <Text style={styles.emptyRankingTitle}>
                {selectedTeamFilter === 'ALL' ? '전체 랭킹' : `${selectedTeamFilter.split(' ')[0]} 팀 랭킹`}
              </Text>
              <Text style={styles.emptyRankingText}>아직 랭킹 데이터가 없습니다</Text>
              <Text style={styles.emptyRankingSubtext}>경기 결과가 집계되면 랭킹이 표시됩니다</Text>
            </View>
          )}

          {/* 4-10위 일반 리스트 */}
          {restUsers.map((user) => {
            const team = findTeamById(user.userTeamId || 0);
            return (
              <View key={user.rank} style={styles.userRankingItem}>
                <View style={styles.userLeftInfo}>
                  <Text style={styles.userRank}>{user.rank}</Text>
                  {team && (
                    <Image 
                      source={team.imagePath}
                      style={styles.userTeamLogo}
                      resizeMode="contain"
                    />
                  )}
                  <Text style={styles.username}>{user.nickname}</Text>
                </View>
                <View style={styles.userStats}>
                  <Text style={styles.userWinRate}>{(user.winRate * 100).toFixed(1)}%</Text>
                </View>
              </View>
            );
          })}
          
          {/* 내 정보 (10위 밖인 경우) */}
          {currentUserRanking && currentUserRanking.rank > 10 && (() => {
            const team = findTeamById(currentUserRanking.userTeamId || 0);
            return (
              <>
                <View style={styles.divider}>
                  <Text style={styles.dividerText}>...</Text>
                </View>
                <View style={[styles.userRankingItem, styles.currentUserItem]}>
                  <View style={styles.userLeftInfo}>
                    <Text style={styles.userRank}>{currentUserRanking.rank}</Text>
                    {team && (
                      <Image 
                        source={team.imagePath}
                        style={styles.userTeamLogo}
                        resizeMode="contain"
                      />
                    )}
                    <Text style={styles.username}>{currentUserRanking.nickname}</Text>
                  </View>
                  <View style={styles.userStats}>
                    <Text style={styles.userWinRate}>{(currentUserRanking.winRate * 100).toFixed(1)}%</Text>
                    {currentUserRanking.percentile && (
                      <Text style={styles.userPercentile}>상위 {(100 - currentUserRanking.percentile).toFixed(1)}%</Text>
                    )}
                  </View>
                </View>
              </>
            );
          })()}
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
    marginRight: 12,
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 25,
    backgroundColor: '#f8f9fa',
    borderWidth: 2,
    borderColor: 'transparent',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    minWidth: 80,
  },
  selectedTeamFilter: {
    borderColor: 'transparent',
    shadowOpacity: 0.2,
    shadowRadius: 6,
    elevation: 6,
    transform: [{ scale: 1.05 }],
  },
  teamLogoContainer: {
    alignItems: 'center',
  },
  teamFilterLogo: {
    width: 40,
    height: 40,
    marginBottom: 4,
  },
  selectedTeamLogo: {
    width: 44,
    height: 44,
  },
  allFilterContainer: {
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 8,
  },
  allFilterText: {
    fontSize: 16,
    color: '#666666',
    fontWeight: '600',
  },
  selectedAllFilterText: {
    color: '#ffffff',
    fontWeight: 'bold',
  },
  teamFilterName: {
    fontSize: 12,
    color: '#666666',
    fontWeight: '600',
    textAlign: 'center',
  },
  selectedTeamFilterName: {
    color: '#ffffff',
    fontWeight: 'bold',
  },
  userRankingList: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 20,
  },
  scrollContent: {
    paddingBottom: 60,
    flexGrow: 1,
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
  userLeftInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  userRank: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333333',
    width: 30,
    marginRight: 8,
  },
  username: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333333',
    marginLeft: 8,
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
  userPercentile: {
    fontSize: 12,
    color: '#666666',
    marginTop: 2,
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
    paddingVertical: 30,
    paddingHorizontal: 20,
    backgroundColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    marginBottom: 20,
    borderRadius: 20,
    marginHorizontal: 16,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 8,
    },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 15,
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
  crownContainer: {
    width: 60,
    height: 60,
    borderRadius: 30,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 6,
    elevation: 8,
    position: 'relative',
  },
  crownText: {
    fontSize: 24,
    position: 'absolute',
    top: -8,
  },
  rankNumber: {
    color: '#ffffff',
    fontSize: 20,
    fontWeight: 'bold',
    marginTop: 8,
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
    maxWidth: 90,
    justifyContent: 'center',
    alignItems: 'center',
    borderTopLeftRadius: 15,
    borderTopRightRadius: 15,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.3,
    shadowRadius: 6,
    elevation: 8,
    position: 'relative',
  },
  podiumRank: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: 'bold',
    zIndex: 1,
  },
  sparkles: {
    position: 'absolute',
    top: 10,
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'space-around',
    zIndex: 1,
  },
  sparkle: {
    fontSize: 16,
    opacity: 0.8,
  },
  emptyRankingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 80,
    paddingHorizontal: 40,
  },
  emptyRankingIcon: {
    fontSize: 64,
    marginBottom: 20,
    opacity: 0.6,
  },
  emptyRankingTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 8,
    textAlign: 'center',
  },
  emptyRankingText: {
    fontSize: 16,
    color: '#666666',
    marginBottom: 8,
    textAlign: 'center',
  },
  emptyRankingSubtext: {
    fontSize: 14,
    color: '#999999',
    textAlign: 'center',
    lineHeight: 20,
  },
  podiumUserInfo: {
    alignItems: 'center',
    marginBottom: 8,
  },
  podiumTeamLogo: {
    width: 20,
    height: 20,
    marginBottom: 4,
  },
  userTeamLogo: {
    width: 16,
    height: 16,
    marginRight: 8,
  },
});