import React, { useEffect, useState, useMemo } from 'react';
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
import { useRem, useMinTouch } from '../../../shared/ui/atoms/button/responsive';

type Props = {
  teamLogo?: ImageSourcePropType;
  teamName: string;
  rankText: string;
  recordText: string;
  onPressChat?: () => void;
  accentColor?: string;        // 팀 색상
  withSafeAreaTop?: boolean;   // 상단 안전영역 패딩 적용 여부
  topExtra?: number;           // 헤더를 더 내리고 싶을 때 추가 여백(px)

  /** 우측 고정 영역(채팅 버튼 아래)에 함께 표시할 외부 액션들(예: 팀 최신 뉴스 버튼) */
  rightExtras?: React.ReactNode;
};

// Zustand selector (컴포넌트 바깥)
const selectIsVerifiedToday = (state: any) => state.isVerifiedToday;
const selectTodayGameInfo = (state: any) => state.todayGameInfo;

function TeamHeaderCard({
  teamLogo,
  teamName,
  rankText,
  recordText,
  onPressChat,
  accentColor = '#E85A5A',
  withSafeAreaTop = true,
  topExtra = 30,
  rightExtras,
}: Props) {
  const rem = useRem();
  const minTouch = useMinTouch();
  const insets = useSafeAreaInsets();

  const topInset = withSafeAreaTop
    ? Math.max(insets.top, Platform.OS === 'android' ? (StatusBar.currentHeight ?? 0) : 0)
    : 0;
  const headerTop = topInset + topExtra;

  const isVerifiedToday = useAttendanceStore(selectIsVerifiedToday);
  const todayGameInfo = useAttendanceStore(selectTodayGameInfo);
  const [gameInfo, setGameInfo] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showButton, setShowButton] = useState(true);

  const isVerified = useMemo(() => false, []);

  // 오늘 경기 로드 - 목 데이터 사용
  useEffect(() => {
    const loadTodayGame = async () => {
      setIsLoading(false);
      // 목 데이터 설정
      const mockGameInfo = {
        gameId: 1,
        homeTeamName: 'LG 트윈스',
        awayTeamName: '두산 베어스',
        dateTime: new Date().toISOString(),
        stadium: '잠실야구장'
      };
      setGameInfo(mockGameInfo);
      setShowButton(true);
    };
    loadTodayGame();
  }, [isVerified, todayGameInfo]);

  // 경기 2시간 전부터 버튼 활성화 (초기 한 번만 체크)
  useEffect(() => {
    if (!gameInfo || isVerified) return;
    const gameDateTime = new Date(gameInfo.dateTime);
    const now = new Date();
    const twoHoursBefore = new Date(gameDateTime.getTime() - 2 * 60 * 60 * 1000);
    setShowButton(now >= twoHoursBefore);
  }, [gameInfo, isVerified]);

  const getChatButtonText = () => {
    return '직관인증하기';
  };

  // 버튼 항상 활성화 (테스트용)
  const enabledNow = true;

  return (
    <View
      style={[
        s.wrap,
        {
          backgroundColor: accentColor,
          paddingTop: headerTop,
          paddingHorizontal: Math.round(rem(1.25)), // 20dp 기준
        },
      ]}
    >
      <StatusBar barStyle="light-content" backgroundColor={accentColor} />

      {/* 좌측 팀 정보 */}
      <View style={s.left}>
        <Image
          source={typeof teamLogo === 'string' ? { uri: teamLogo } : teamLogo}
          style={{ width: rem(3.25), height: rem(3.25), borderRadius: rem(1.625), backgroundColor: '#fff' }}
          resizeMode="contain"
        />
        <View style={{ marginLeft: rem(0.75) }}>
          <Text style={{ color: '#fff', fontSize: rem(1.125), fontWeight: '800' }}>{teamName}</Text>
          <Text style={{ color: '#fff', marginTop: 2, fontWeight: '700' }}>{rankText}</Text>
          <Text style={{ color: 'rgba(255,255,255,0.85)', marginTop: 2, fontSize: rem(0.75) }}>{recordText}</Text>
        </View>
      </View>

      {/* 우측 고정 영역(헤더 top 계산 공유) */}
      <View style={[s.rightFixed, { top: headerTop - 12, right: Math.round(rem(1.25)) }]}>
        <Pressable
          style={[
            s.chat,
            {
              paddingVertical: Math.max(8, (minTouch - 32) / 2),
              paddingHorizontal: rem(0.625),
              maxWidth: Math.min(160, rem(8.25)),
              opacity: !enabledNow ? 0.6 : 1,
            },
          ]}
          onPress={onPressChat}
          disabled={false}
          hitSlop={8}
        >
          <Text
            style={{
              color: '#000',
              fontSize: rem(0.75),
              fontWeight: '600',
              textAlign: 'center',
              lineHeight: rem(0.9),
            }}
            numberOfLines={2}
            adjustsFontSizeToFit
          >
            {getChatButtonText()}
          </Text>
        </Pressable>

        {/* 외부에서 꽂아 넣는 우측 버튼들 (예: 팀 최신 뉴스) */}
        {rightExtras ? <View style={{ marginTop: 8 }}>{rightExtras}</View> : null}
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  wrap: {
    paddingBottom: 16,
    justifyContent: 'flex-start',
  },
  left: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  rightFixed: {
    position: 'absolute',
    alignItems: 'flex-end',
  },
  chat: {
    backgroundColor: '#fff',
    borderRadius: 10,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 3 },
    elevation: 3,
  },
});

export default React.memo(TeamHeaderCard);
