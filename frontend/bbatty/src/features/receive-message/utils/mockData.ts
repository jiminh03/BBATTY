import { ChatMessage, GameChatMessage, MatchChatMessage, SystemMessage } from '../model/types';

// 목업 사용자 데이터
const mockUsers = [
  { id: '1', nickname: '축구왕', profileImg: null, isVictoryFairy: false },
  { id: '2', nickname: '골키퍼마스터', profileImg: null, isVictoryFairy: true },
  { id: '3', nickname: '패스의달인', profileImg: null, isVictoryFairy: false },
  { id: '4', nickname: '드리블러', profileImg: null, isVictoryFairy: false },
  { id: '5', nickname: '승리요정', profileImg: null, isVictoryFairy: true },
];

// 현재 사용자 (테스트용)
export const MOCK_CURRENT_USER_ID = '1';

// 메시지 내용 풀
const messageContents = [
  '안녕하세요!',
  '오늘 경기 어떻게 보세요?',
  '우리 팀이 이길 것 같아요',
  '상대팀도 만만치 않네요',
  '골 들어갑니다!',
  '아쉽네요 ㅠㅠ',
  '다음 기회에!',
  '응원합니다!',
  '화이팅!',
  '좋은 경기였어요',
  '다들 수고하셨습니다',
  '재미있었네요 ㅎㅎ',
  '또 만나요~',
  '경기 분석해보면...',
  '선수들 컨디션이 좋아보여요',
  '전술이 잘 맞는 것 같아요',
  '감독의 판단이 좋았네요',
  '이런 경기 또 보고 싶어요',
  '스릴 넘치는 경기였어요',
  '마지막까지 긴장감이...'
];

// 시스템 메시지 내용
const systemMessages = [
  '경기가 시작되었습니다.',
  '전반전이 종료되었습니다.',
  '후반전이 시작되었습니다.',
  '경기가 종료되었습니다.',
  '득점이 발생했습니다!',
  '교체가 있었습니다.',
];

// 랜덤 시간 생성 (최근 2시간 내)
const getRandomTimestamp = (baseTime?: Date): string => {
  const base = baseTime || new Date();
  const randomMinutes = Math.floor(Math.random() * 120); // 0~120분 전
  const timestamp = new Date(base.getTime() - randomMinutes * 60 * 1000);
  return timestamp.toISOString();
};

// 매칭 채팅 메시지 생성
const createMockMatchMessage = (index: number, baseTime?: Date): MatchChatMessage => {
  const user = mockUsers[index % mockUsers.length];
  const content = messageContents[Math.floor(Math.random() * messageContents.length)];
  
  return {
    type: 'message',
    messageId: `match_msg_${index}`,
    roomId: 'room123',
    userId: user.id,
    nickname: user.nickname,
    content,
    timestamp: getRandomTimestamp(baseTime),
    messageType: 'CHAT',
    profileImgUrl: user.profileImg ?? undefined,
    isVictoryFairy: user.isVictoryFairy,
  };
};

// 게임 채팅 메시지 생성 (익명)
const createMockGameMessage = (index: number, baseTime?: Date): GameChatMessage => {
  const content = messageContents[Math.floor(Math.random() * messageContents.length)];
  
  return {
    type: 'message',
    messageId: `game_msg_${index}`,
    roomId: 'room123',
    content,
    timestamp: getRandomTimestamp(baseTime),
    messageType: 'CHAT',
    chatType: 'game',
    teamId: Math.random() > 0.5 ? 'team1' : 'team2',
  };
};

// 시스템 메시지 생성
const createMockSystemMessage = (index: number, baseTime?: Date): SystemMessage => {
  const isJoin = Math.random() > 0.5;
  const user = mockUsers[Math.floor(Math.random() * mockUsers.length)];
  
  return {
    type: isJoin ? 'user_join' : 'user_leave',
    messageId: `system_msg_${index}`,
    content: isJoin ? `${user.nickname}님이 입장했습니다.` : `${user.nickname}님이 퇴장했습니다.`,
    timestamp: getRandomTimestamp(baseTime),
    messageType: 'SYSTEM',
    userId: user.id,
    userName: user.nickname,
  };
};

// 초기 메시지 생성 (최근 50개)
export const generateInitialMessages = (count: number = 50, type: 'match' | 'game' = 'match'): ChatMessage[] => {
  const messages: ChatMessage[] = [];
  const now = new Date();
  
  for (let i = 0; i < count; i++) {
    const baseTime = new Date(now.getTime() - i * 2 * 60 * 1000); // 2분 간격
    
    // 90% 확률로 일반 메시지, 10% 확률로 시스템 메시지
    if (Math.random() > 0.1) {
      if (type === 'match') {
        messages.push(createMockMatchMessage(i, baseTime));
      } else {
        messages.push(createMockGameMessage(i, baseTime));
      }
    } else {
      messages.push(createMockSystemMessage(i, baseTime));
    }
  }
  
  // 시간순 정렬 (오래된 것부터)
  return messages.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
};

// 히스토리 메시지 생성 (더보기용)
export const generateHistoryMessages = (
  lastTimestamp: number, 
  count: number = 50, 
  type: 'match' | 'game' = 'match'
): ChatMessage[] => {
  const messages: ChatMessage[] = [];
  const baseTime = new Date(lastTimestamp);
  
  for (let i = 1; i <= count; i++) {
    const messageTime = new Date(baseTime.getTime() - i * 3 * 60 * 1000); // 3분 간격
    
    if (Math.random() > 0.1) {
      if (type === 'match') {
        messages.push(createMockMatchMessage(Date.now() + i, messageTime));
      } else {
        messages.push(createMockGameMessage(Date.now() + i, messageTime));
      }
    } else {
      messages.push(createMockSystemMessage(Date.now() + i, messageTime));
    }
  }
  
  return messages.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
};

// 새 메시지 생성 (실시간 시뮬레이션용)
export const generateNewMessage = (type: 'match' | 'game' = 'match'): ChatMessage => {
  const random = Math.random();
  
  if (random > 0.1) {
    if (type === 'match') {
      return createMockMatchMessage(Date.now(), new Date());
    } else {
      return createMockGameMessage(Date.now(), new Date());
    }
  } else {
    return createMockSystemMessage(Date.now(), new Date());
  }
};