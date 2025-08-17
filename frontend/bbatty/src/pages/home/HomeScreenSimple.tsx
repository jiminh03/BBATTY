import React from 'react';
import { View, Text } from 'react-native';
import { HomeStackScreenProps } from '../../navigation/types';
import { useUserStore } from '../../entities/user/model/userStore';
import { useAttendanceStore } from '../../entities/attendance/model/attendanceStore';
import { useTeamStanding } from '../../entities/team/queries/useTeamStanding';
import {
  useTeamPopularPostsQuery,
  usePostListQuery,
} from '../../entities/post/queries/usePostQueries';
import { useSearchHistoryStore } from '../../entities/post/model/searchHistoryStore';

type Props = HomeStackScreenProps<'Home'>;

// Zustand selector 함수들을 컴포넌트 외부에 정의하여 캐시
const selectTeamId = (state: any) => state.currentUser?.teamId ?? 1;
const selectIsVerifiedToday = (state: any) => state.isVerifiedToday;
const selectAddHistory = (state: any) => state.addHistory;
const selectRemoveHistory = (state: any) => state.removeHistory;

// 더 안정적인 selector 방식
const selectHistoriesByTeam = (state: any) => state.historiesByTeam;

function HomeScreenSimple({ navigation }: Props) {
  console.log('🔄 HomeScreenSimple 리렌더링됨');
  
  const teamId = useUserStore(selectTeamId);
  const isVerifiedToday = useAttendanceStore(selectIsVerifiedToday);
  const { data: standing } = useTeamStanding(teamId);
  
  // Post 관련 queries
  const { data: popular = [], isLoading: pLoading } = useTeamPopularPostsQuery(teamId, 20);
  const listQ = usePostListQuery(teamId);
  
  // SearchHistory store
  const addHistory = useSearchHistoryStore(selectAddHistory);
  const removeHistory = useSearchHistoryStore(selectRemoveHistory);
  const historiesByTeam = useSearchHistoryStore(selectHistoriesByTeam);
  const history = historiesByTeam[teamId] ?? [];
  
  console.log('📊 팀ID:', teamId, 'standing:', !!standing, 'popular:', popular.length, 'listQ:', !!listQ.data, 'history:', history.length);
  
  return (
    <View style={{ flex: 1, backgroundColor: '#fff', justifyContent: 'center', alignItems: 'center' }}>
      <Text>Simple Home Screen</Text>
      <Text>팀 ID: {teamId}</Text>
      <Text>인증 여부: {String(typeof isVerifiedToday)}</Text>
      <Text>순위 데이터: {standing ? `${standing.rank}위` : '로딩중'}</Text>
      <Text>인기글: {popular.length}개</Text>
      <Text>전체글: {listQ.data?.pages?.length || 0}페이지</Text>
      <Text>검색 히스토리: {history.length}개</Text>
    </View>
  );
}

export default React.memo(HomeScreenSimple);