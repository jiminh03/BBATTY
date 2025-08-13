import React, { useMemo, useState } from 'react';
import { View, FlatList, ActivityIndicator, StyleSheet, TouchableOpacity } from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import TeamHeaderCard from '../../entities/team/ui/TeamHeaderCard';
import SegmentTabs from '../../entities/team/ui/SegmentTabs';
import { PostItem } from '../../entities/post/ui/PostItem';
import { useTeamPopularPostsQuery, usePostListQuery } from '../../entities/post/queries/usePostQueries';
import { useUserStore } from '../../entities/user/model/userStore';
import { useAttendanceStore } from '../../entities/attendance/model/attendanceStore';
import TeamGearIcon from '../../shared/ui/atoms/Team/TeamGearIcon';
import { findTeamById } from '../../shared/team/teamTypes';
import { useTeamStanding } from '../../entities/team/queries/useTeamStanding';

type Props = HomeStackScreenProps<'Home'>;

export default function HomeScreen({ navigation }: Props) {
  const teamId = useUserStore((s) => s.currentUser?.teamId) ?? 1;
  const { isVerifiedToday } = useAttendanceStore();
  const team = findTeamById(teamId);
  const teamColor = team?.color ?? '#1D467F';

  // 현재 순위/전적 가져오기
  const { data: standing } = useTeamStanding(teamId);
  const rankText = standing ? `${standing.rank}위` : '순위 준비중';
  const recordText = standing
    ? `${standing.wins}승 ${standing.draws}무 ${standing.losses}패 (${standing.winRate.toFixed(3)})`
    : '전적 준비중';

  const [tab, setTab] = useState<'best'|'all'>('best');

  const { data: popular = [], isLoading: pLoading } = useTeamPopularPostsQuery(teamId, 20);
  const listQ = usePostListQuery(teamId);
  const allPosts = useMemo(() => (listQ.data?.pages ?? []).flatMap(p => p.posts ?? []), [listQ.data]);

  const data = tab === 'best' ? popular : allPosts;
  const isFetchingNext = tab === 'all' ? listQ.isFetchingNextPage : false;
  const hasNext = tab === 'all' ? (listQ.hasNextPage ?? false) : false;

  const handleChatPress = async () => {
    const isVerified = isVerifiedToday();
    if (isVerified) {
      try {
        // 오늘의 게임 정보 가져오기
        const { gameApi } = await import('../../entities/game/api/api');
        const todayGameResponse = await gameApi.getTodayGame();
        
        if (todayGameResponse.status === 'SUCCESS' && todayGameResponse.data) {
          const todayGame = todayGameResponse.data;
          
          // 직관채팅으로 이동 (매치채팅 목록으로 이동 후 직접 연결)
          navigation.navigate('MatchChatStack', {
            screen: 'MatchChatRoomList',
            params: {
              directWatchConnection: {
                gameId: todayGame.gameId,
                teamId: teamId,
                isAttendanceVerified: true,
              },
            },
          });
        } else {
          console.error('오늘의 게임 정보를 가져올 수 없습니다.');
          // 직관 인증이 되어있지만 게임 정보가 없으면 일반 채팅 목록으로
          navigation.navigate('MatchChatStack', { screen: 'MatchChatRoomList' });
        }
      } catch (error) {
        console.error('게임 정보 로드 실패:', error);
        // 에러 발생시 일반 채팅 목록으로
        navigation.navigate('MatchChatStack', { screen: 'MatchChatRoomList' });
      }
    } else {
      // 직관 인증 화면으로 이동
      navigation.navigate('AttendanceVerification' as never);
    }
  };

  return (
    <View style={{ flex: 1, backgroundColor: '#fff' }}>
      <TeamHeaderCard
        teamLogo={String(team?.imagePath ?? '')}
        teamName={team?.name ?? 'KBO 팀'}
        rankText={rankText}
        recordText={recordText}
        onPressChat={handleChatPress}
        accentColor={teamColor}
      />

      <SegmentTabs value={tab} onChange={setTab} />

      {tab === 'best' ? (
        pLoading ? (
          <ActivityIndicator style={{ marginTop: 16 }} />
        ) : (
          <FlatList
            data={data}
            keyExtractor={i => String(i.id)}
            renderItem={({ item }) => (
              <PostItem
                post={item}
                onPress={() => navigation.navigate('PostDetail', { postId: item.id })}
              />
            )}
            contentContainerStyle={styles.listPad}
          />
        )
      ) : (
        <FlatList
          data={data}
          keyExtractor={i => String(i.id)}
          renderItem={({ item }) => (
            <PostItem
              post={item}
              onPress={() => navigation.navigate('PostDetail', { postId: item.id })}
            />
          )}
          onEndReachedThreshold={0.35}
          onEndReached={() => {
            if (hasNext && !isFetchingNext) listQ.fetchNextPage();
          }}
          ListFooterComponent={
            isFetchingNext ? <ActivityIndicator style={{ marginVertical: 12 }} /> : <View />
          }
          contentContainerStyle={styles.listPad}
        />
      )}

      {/* ↓↓↓ 흰색 원 배경 + 그림자 위에 팀색 SVG 아이콘 */}
      <TouchableOpacity
        style={styles.fab}
        onPress={() => navigation.navigate('PostForm' as never)}
        activeOpacity={0.85}
        accessibilityLabel="게시글 작성"
      >
        <View style={styles.fabCircle}>
          <TeamGearIcon teamId={teamId} size={44} />
        </View>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  listPad: { paddingBottom: 16 },
  fab: {
    position: 'absolute',
    right: 16,
    bottom: 24,
  },
  fabCircle: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#FFFFFF',          // ← 흰색 배경
    alignItems: 'center',
    justifyContent: 'center',
    // iOS shadow
    shadowColor: '#000',
    shadowOpacity: 0.15,
    shadowOffset: { width: 0, height: 4 },
    shadowRadius: 8,
    // Android shadow
    elevation: 6,
  },
});
