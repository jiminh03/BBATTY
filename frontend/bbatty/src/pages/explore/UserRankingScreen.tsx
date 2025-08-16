import React, { useState, useRef, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Image, Alert, ActivityIndicator } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { ExploreStackScreenProps } from '../../navigation/types';
import { TEAMS, findTeamById } from '../../shared/team/teamTypes';
import { useThemeColor } from '../../shared/team/ThemeContext';
import { useUserStore } from '../../entities/user/model/userStore';
import { useGlobalRanking, useAllTeamRankings, UserRanking } from '../../entities/ranking/queries/useRankingQueries';

// 타입들은 useRankingQueries에서 import

type Props = ExploreStackScreenProps<'UserRanking'>;

const STORAGE_KEY = 'userRanking_selectedTeam';

export default function UserRankingScreen({ navigation, route }: Props) {
  const [selectedTeamFilter, setSelectedTeamFilter] = useState<string>('ALL');
  const themeColor = useThemeColor();
  const getCurrentUser = useUserStore((state) => state.getCurrentUser);
  const currentUser = getCurrentUser();
  const scrollViewRef = useRef<ScrollView>(null);
  const rankingScrollViewRef = useRef<ScrollView>(null);

  // 선택된 팀 필터 상태를 로컬 스토리지에서 불러오기
  useEffect(() => {
    const loadSelectedTeam = async () => {
      try {
        const savedTeam = await AsyncStorage.getItem(STORAGE_KEY);
        if (savedTeam) {
          setSelectedTeamFilter(savedTeam);
        }
      } catch (error) {
        console.error('선택된 팀 필터 로드 실패:', error);
      }
    };
    loadSelectedTeam();
  }, []);

  // 팀 필터 변경 시 로컬 스토리지에 저장
  const handleTeamFilterChange = async (teamId: string) => {
    try {
      setSelectedTeamFilter(teamId);
      await AsyncStorage.setItem(STORAGE_KEY, teamId);
    } catch (error) {
      console.error('선택된 팀 필터 저장 실패:', error);
    }
  };

  // TanStack Query 훅 사용
  const { 
    data: globalRankingData, 
    isLoading: globalLoading, 
    error: globalError 
  } = useGlobalRanking();

  const teamIds = TEAMS.map(team => team.id);
  const { 
    data: allTeamRankingsData, 
    isLoading: teamRankingsLoading, 
    error: teamRankingsError 
  } = useAllTeamRankings(teamIds);

  // 로딩 상태 통합
  const loading = globalLoading || teamRankingsLoading;

  // 에러 처리
  if (globalError || teamRankingsError) {
    console.error('랭킹 조회 실패:', globalError || teamRankingsError);
  }

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
        rankings: globalRankingData?.rankings || [],
        myRanking: globalRankingData?.myRanking || null
      };
    } else {
      const teamRankingsData = allTeamRankingsData?.teamRankingsData || {};
      const myTeamRankings = allTeamRankingsData?.myTeamRankings || {};
      
      return {
        rankings: teamRankingsData[selectedTeamFilter] || [],
        myRanking: myTeamRankings[selectedTeamFilter] || null
      };
    }
  };

  // 에러 상태 처리
  if ((globalError || teamRankingsError) && !globalRankingData && !allTeamRankingsData) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorIcon}>⚠️</Text>
        <Text style={styles.errorText}>랭킹 정보를 불러올 수 없습니다</Text>
        <Text style={styles.errorSubText}>네트워크 연결을 확인해주세요</Text>
        <TouchableOpacity 
          style={[styles.retryButton, { backgroundColor: themeColor }]}
          onPress={() => {
            // 쿼리 재시작은 자동으로 처리됨
          }}
        >
          <Text style={styles.retryButtonText}>다시 시도</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={themeColor} />
        <Text style={styles.loadingText}>랭킹 정보를 불러오는 중...</Text>
        <Text style={styles.loadingSubText}>잠시만 기다려주세요</Text>
      </View>
    );
  }

  const { rankings: currentRankings, myRanking: currentMyRanking } = getCurrentRankingData();
  
  // 현재 사용자 정보 먼저 찾기
  const currentUserRanking = currentRankings.find(user => user.isCurrentUser) || currentMyRanking;
  
  // Top 3 사용자 (현재 사용자 포함)
  const top3Users = currentRankings.filter(user => user.rank <= 3);
  
  // 4-10위 사용자 (현재 사용자 포함)
  const restUsers = currentRankings.filter(user => user.rank > 3 && user.rank <= 10);

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
        <ScrollView 
          ref={rankingScrollViewRef}
          style={styles.userRankingList}
          contentContainerStyle={styles.scrollContent}
          stickyHeaderIndices={[0]}
        >
          {/* 팀 필터 - 타팀 커뮤니티와 완전히 동일한 탭 디자인 */}
          <View style={styles.teamPickerWrap}>
            <ScrollView
              ref={scrollViewRef}
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.teamPickerContent}
            >
              {teamFilters.map((filter) => {
                const isActive = selectedTeamFilter === filter.id;
                return (
                  <TouchableOpacity
                    key={filter.id}
                    style={[styles.teamChip, isActive && styles.teamChipActive]}
                    onPress={() => handleTeamFilterChange(filter.id)}
                    activeOpacity={0.85}
                  >
                    {filter.logo ? (
                      <>
                        <Image
                          source={typeof filter.logo === 'string' ? { uri: filter.logo } : filter.logo}
                          style={styles.teamLogo}
                          resizeMode="contain"
                        />
                        <Text style={[styles.teamChipLabel, isActive && { color: themeColor }]}>
                          {filter.name}
                        </Text>
                      </>
                    ) : (
                      <Text style={[styles.teamChipLabel, isActive && { color: themeColor }]}>
                        {filter.name}
                      </Text>
                    )}
                  </TouchableOpacity>
                );
              })}
            </ScrollView>
          </View>
          
          {/* 여백 추가 - 팀 필터와 순위표 사이 */}
          <View style={styles.spacer} />
          
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
            const isCurrentUser = user.isCurrentUser;
            return (
              <View key={user.rank} style={[
                styles.userRankingItem, 
                isCurrentUser && styles.currentUserItem
              ]}>
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
                  {isCurrentUser && user.percentile && (
                    <Text style={styles.userPercentile}>상위 {(100 - user.percentile).toFixed(1)}%</Text>
                  )}
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
    backgroundColor: '#ffffff',
  },
  loadingText: {
    fontSize: 16,
    color: '#333333',
    fontWeight: '600',
    marginTop: 16,
  },
  loadingSubText: {
    fontSize: 14,
    color: '#666666',
    marginTop: 8,
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 40,
    paddingHorizontal: 40,
    backgroundColor: '#ffffff',
  },
  errorIcon: {
    fontSize: 48,
    marginBottom: 16,
  },
  errorText: {
    fontSize: 18,
    color: '#333333',
    fontWeight: '600',
    textAlign: 'center',
    marginBottom: 8,
  },
  errorSubText: {
    fontSize: 14,
    color: '#666666',
    textAlign: 'center',
    marginBottom: 24,
  },
  retryButton: {
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  retryButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  userRankingContainer: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  teamPickerWrap: {
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#eee',
    backgroundColor: '#fff',
  },
  teamPickerContent: { paddingHorizontal: 12, paddingVertical: 10, gap: 10, justifyContent: 'center' },
  teamChip: {
    height: 64, minWidth: 86, paddingHorizontal: 10, paddingVertical: 8,
    backgroundColor: '#F6F7F8', borderRadius: 12,
    borderWidth: StyleSheet.hairlineWidth, borderColor: '#E3E5E7',
    alignItems: 'center', justifyContent: 'center',
  },
  teamChipActive: {
    backgroundColor: '#fff', borderColor: '#cfd3d7',
    shadowColor: '#000', shadowOpacity: 0.08, shadowOffset: { width: 0, height: 2 },
    shadowRadius: 4, elevation: 3,
  },
  teamChipLabel: { fontSize: 12, color: '#666', fontWeight: '600' },
  userRankingList: {
    flex: 1,
    paddingHorizontal: 0,
    paddingTop: 0,
  },
  scrollContent: {
    paddingBottom: 60,
    paddingHorizontal: 16,
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
    width: 40,
    height: 40,
    marginBottom: 4,
  },
  userTeamLogo: {
    width: 32,
    height: 32,
    marginRight: 8,
  },
  spacer: {
    height: 80,
  },
  teamLogo: { width: 36, height: 36, marginBottom: 4 },
});