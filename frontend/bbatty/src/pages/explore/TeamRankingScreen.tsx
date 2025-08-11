import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, Image } from 'react-native';
import { ExploreStackScreenProps } from '../../navigation/types';
import { TEAMS } from '../../shared/team/teamTypes';
import { useTokenStore } from '../../shared/api/token/tokenStore';

interface TeamRanking {
  teamName: string;
  games: number;
  wins: number;
  draws: number;
  losses: number;
  winRate: number;
  gameBehind: number;
  streak: number;
  streakText: string;
}

type Props = ExploreStackScreenProps<'TeamRanking'>;

export default function TeamRankingScreen({ navigation, route }: Props) {
  const [teamRankings, setTeamRankings] = useState<TeamRanking[]>([]);
  const [loading, setLoading] = useState(false);
  const { getAccessToken } = useTokenStore();

  const fetchTeamRankings = async () => {
    try {
      setLoading(true);
      const accessToken = getAccessToken();
      
      if (!accessToken) {
        throw new Error('No access token');
      }
      
      const response = await fetch('http://i13a403.p.ssafy.io:8080/api/teams/ranking', {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${accessToken}`,
        },
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const result = await response.json();
      
      if (result.status === 'SUCCESS' && result.data) {
        setTeamRankings(result.data);
      }
    } catch (error) {
      setTeamRankings([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTeamRankings();
  }, []);

  const getTeamInfo = (teamName: string) => {
    return TEAMS.find(t => t.name === teamName);
  };


  const getShortTeamName = (teamName: string) => {
    return teamName.split(' ')[0];
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingText}>로딩 중...</Text>
      </View>
    );
  }

  return (
    <View style={styles.tableContainer}>
        <View style={styles.tableHeader}>
          <Text style={[styles.headerCell, { width: 25 }]}>순위</Text>
          <Text style={[styles.headerCell, { width: 100 }]}>팀</Text>
          <Text style={[styles.headerCell, { width: 40 }]}>경기</Text>
          <Text style={[styles.headerCell, { width: 35 }]}>승</Text>
          <Text style={[styles.headerCell, { width: 25 }]}>무</Text>
          <Text style={[styles.headerCell, { width: 35 }]}>패</Text>
          <Text style={[styles.headerCell, { width: 50 }]}>승률</Text>
          <Text style={[styles.headerCell, { width: 35 }]}>게임차</Text>
          <Text style={[styles.headerCell, { width: 65 }]}>최근</Text>
        </View>
        
        <ScrollView showsVerticalScrollIndicator={false}>
          {teamRankings.map((team, index) => {
            const teamInfo = getTeamInfo(team.teamName);
            return (
              <View key={team.teamName} style={styles.tableRow}>
                <Text style={[styles.cell, { width: 25 }]}>{index + 1}</Text>
                <View style={[styles.teamCellWithLogo, { width: 100 }]}>
                  {teamInfo && (
                    <Image 
                      source={typeof teamInfo.imagePath === 'string' ? { uri: teamInfo.imagePath } : teamInfo.imagePath} 
                      style={styles.teamLogo}
                      resizeMode="contain"
                    />
                  )}
                </View>
                <Text style={[styles.cell, { width: 40 }]}>{team.games}</Text>
                <Text style={[styles.cell, { width: 35 }]}>{team.wins}</Text>
                <Text style={[styles.cell, { width: 25 }]}>{team.draws}</Text>
                <Text style={[styles.cell, { width: 35 }]}>{team.losses}</Text>
                <Text style={[styles.cell, { width: 50 }]}>{team.winRate.toFixed(3)}</Text>
                <Text style={[styles.cell, { width: 35 }]}>{team.gameBehind === 0 ? '-' : team.gameBehind}</Text>
                <Text style={[styles.cell, { width: 65, color: team.streak > 0 ? '#007AFF' : '#FF3B30' }]}>
                  {team.streakText}
                </Text>
              </View>
            );
          })}
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
  tableContainer: {
    flex: 1,
    paddingTop: 16,
  },
  tableHeader: {
    flexDirection: 'row',
    backgroundColor: '#f8f9fa',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef',
  },
  headerCell: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#495057',
    textAlign: 'center',
  },
  tableRow: {
    flexDirection: 'row',
    paddingVertical: 16,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f1f3f4',
    alignItems: 'center',
  },
  cell: {
    fontSize: 13,
    color: '#333333',
    textAlign: 'center',
  },
  teamCellWithLogo: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  teamLogo: {
    width: 60,
    height: 60,
  },
  teamName: {
    fontSize: 12,
    fontWeight: '600',
    color: '#333333',
    flex: 1,
  },
});