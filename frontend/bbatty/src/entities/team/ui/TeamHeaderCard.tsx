import React, { useEffect, useState } from 'react';
import { View, Text, Image, StyleSheet, Pressable, ImageSourcePropType } from 'react-native';
import { useAttendanceStore } from '../../attendance/model/attendanceStore';
import { gameApi } from '../../game/api/api';

type Props = {
  teamLogo?: ImageSourcePropType;
  teamName: string;
  rankText: string;
  recordText: string;
  onPressChat?: () => void;
  accentColor?: string; // ← 팀 색을 외부에서 주입
};

export default function TeamHeaderCard({
  teamLogo,
  teamName,
  rankText,
  recordText,
  onPressChat,
  accentColor = '#E85A5A',
}: Props) {
  const { isVerifiedToday, todayGameInfo } = useAttendanceStore();
  const [gameInfo, setGameInfo] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);

  const isVerified = isVerifiedToday();

  useEffect(() => {
    const loadTodayGame = async () => {
      if (isVerified && todayGameInfo) {
        setGameInfo(todayGameInfo);
        return;
      }

      setIsLoading(true);
      try {
        const response = await gameApi.getTodayGame();
        if (response.status === 'SUCCESS') {
          setGameInfo(response.data);
        }
      } catch (error) {
        console.error('오늘의 게임 정보 로드 실패:', error);
      } finally {
        setIsLoading(false);
      }
    };

    loadTodayGame();
  }, [isVerified, todayGameInfo]);

  const getChatButtonText = () => {
    if (isLoading) {
      return '로딩중...';
    }
    
    if (isVerified && gameInfo) {
      return `${gameInfo.homeTeamName.split(' ')[0]} VS ${gameInfo.awayTeamName.split(' ')[0]}\n실시간 채팅방 가기`;
    }
    
    return '직관인증하기';
  };

  return (
  <View style={[s.wrap, { backgroundColor: accentColor }]}>
    <View style={s.left}>
      <Image
        source={
          typeof teamLogo === 'string'
            ? { uri: teamLogo } // URL일 때
            : teamLogo          // require(...)나 undefined일 때
        }
        style={s.logo}
        resizeMode="contain"
      />
      <View style={{ marginLeft: 12 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Text style={s.team}>{teamName}</Text>
          <View style={s.badge} />
        </View>
        <Text style={s.rank}>{rankText}</Text>
        <Text style={s.record}>{recordText}</Text>
      </View>
    </View>
    <Pressable style={s.chat} onPress={onPressChat} disabled={isLoading}>
      <Text style={s.chatTxt}>{getChatButtonText()}</Text>
    </Pressable>
  </View>
);
}

const s = StyleSheet.create({
  wrap: { padding: 30, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  left: { flexDirection: 'row', alignItems: 'center' },
  logo: { width: 52, height: 52, borderRadius: 26, backgroundColor: '#fff' },
  team: { color: '#fff', fontSize: 18, fontWeight: '800' },
  badge: { width: 14, height: 14, borderRadius: 7, backgroundColor: '#4CD964', marginLeft: 6 },
  rank: { color: '#fff', marginTop: 2, fontWeight: '700' },
  record: { color: 'rgba(255,255,255,0.85)', marginTop: 2, fontSize: 12 },
  chat: { backgroundColor: '#fff', paddingVertical: 8, paddingHorizontal: 10, borderRadius: 10 },
  chatTxt: { color: '#222', fontSize: 12, fontWeight: '700', textAlign: 'center' },
});
