// TeamHeaderCard.tsx
import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  Image,
  StyleSheet,
  Pressable,
  ImageSourcePropType,
  Platform,
  StatusBar,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useAttendanceStore } from '../../attendance/model/attendanceStore';
import { gameApi } from '../../game/api/api';

type Props = {
  teamLogo?: ImageSourcePropType;
  teamName: string;
  rankText: string;
  recordText: string;
  onPressChat?: () => void;
  accentColor?: string;       // 팀 색상
  withSafeAreaTop?: boolean;  // 상단 안전영역 패딩 적용 여부
  topExtra?: number;          // ⬅️ “더 아래로” 내리고 싶을 때 추가 여백(px)
};

export default function TeamHeaderCard({
  teamLogo,
  teamName,
  rankText,
  recordText,
  onPressChat,
  accentColor = '#E85A5A',
  withSafeAreaTop = true,
  topExtra = 30,              // 기본 추가 여백(원하면 더 키워도 됨)
}: Props) {
  const insets = useSafeAreaInsets();
  const topInset =
    withSafeAreaTop
      ? Math.max(insets.top, Platform.OS === 'android' ? (StatusBar.currentHeight ?? 0) : 0)
      : 0;

  const { isVerifiedToday, todayGameInfo } = useAttendanceStore();
  const [gameInfo, setGameInfo] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showButton, setShowButton] = useState(false);

  const isVerified = isVerifiedToday();

  // 오늘 경기 로드
  useEffect(() => {
    const loadTodayGame = async () => {
      if (isVerified && todayGameInfo) {
        setGameInfo(todayGameInfo);
        setShowButton(true);
        return;
      }

      setIsLoading(true);
      try {
        const response = await gameApi.getTodayGame();
        if ((response as any)?.status === 'SUCCESS') {
          setGameInfo((response as any).data);
        }
      } catch (error) {
        console.error('오늘의 게임 정보 로드 실패:', error);
        setShowButton(false);
      } finally {
        setIsLoading(false);
      }
    };

    loadTodayGame();
  }, [isVerified, todayGameInfo]);

  // 경기 2시간 전부터 버튼 활성화
  useEffect(() => {
    if (!gameInfo || isVerified) return;

    const checkTimeAndUpdate = () => {
      const gameDateTime = new Date(gameInfo.dateTime);
      const now = new Date();
      const twoHoursBeforeGame = new Date(gameDateTime.getTime() - 2 * 60 * 60 * 1000);
      setShowButton(now >= twoHoursBeforeGame);
    };

    checkTimeAndUpdate();
    const interval = setInterval(checkTimeAndUpdate, 60_000);
    return () => clearInterval(interval);
  }, [gameInfo, isVerified]);

  const getChatButtonText = () => {
    if (isLoading) return '로딩중...';
    if (isVerified && gameInfo) {
      const home = String(gameInfo.homeTeamName ?? '').split(' ')[0];
      const away = String(gameInfo.awayTeamName ?? '').split(' ')[0];
      return `${home} VS ${away}\n실시간 채팅방 가기`;
    }
    return '직관인증하기';
  };

  // 버튼 활성화 여부(모양은 그대로 유지, 비활성 시 누르기만 막음)
  const enabledNow = isVerified || showButton;

  return (
    <View
      style={[
        s.wrap,
        {
          backgroundColor: accentColor,
          paddingTop: topInset + topExtra, // ⬅️ 여기서 원하는 만큼 더 내릴 수 있음
        },
      ]}
    >
      <StatusBar barStyle="light-content" backgroundColor={accentColor} />
      <View style={s.left}>
        <Image
          source={typeof teamLogo === 'string' ? { uri: teamLogo } : teamLogo}
          style={s.logo}
          resizeMode="contain"
        />
        <View style={{ marginLeft: 12 }}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Text style={s.team}>{teamName}</Text>
          </View>
          <Text style={s.rank}>{rankText}</Text>
          <Text style={s.record}>{recordText}</Text>
        </View>
      </View>

      <Pressable
        style={s.chat}                            // 🔥 투명도(Opacity) 제거 — 항상 진한 흰색
        onPress={onPressChat}
        disabled={isLoading || !enabledNow}       // 비활성 조건만 걸어줌(모양은 동일)
      >
        <Text style={s.chatTxt}>{getChatButtonText()}</Text>
      </Pressable>
    </View>
  );
}

const s = StyleSheet.create({
  wrap: {
    paddingHorizontal: 20,
    paddingBottom: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  left: { flexDirection: 'row', alignItems: 'center', flex: 1 },
  logo: { width: 52, height: 52, borderRadius: 26, backgroundColor: '#fff' },
  team: { color: '#fff', fontSize: 18, fontWeight: '800' },
  rank: { color: '#fff', marginTop: 2, fontWeight: '700' },
  record: { color: 'rgba(255,255,255,0.85)', marginTop: 2, fontSize: 12 },
  chat: {
    backgroundColor: '#fff',
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 10,
    maxWidth: 132,
    marginTop: 4,
    // 필요하면 약간의 그림자
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 3 },
    elevation: 3,
  },
  chatTxt: {
    color: '#000000ff',
    fontSize: 11,
    fontWeight: '600',
    textAlign: 'center',
    lineHeight: 14,
  },
});
