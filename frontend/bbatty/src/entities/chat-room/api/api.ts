import { apiClient, chatApiClient } from '../../../shared/api';
import { 
  AuthResponse, 
  MatchChatJoinRequest, 
  WatchChatJoinRequest,
  MatchChatRoomsResponse,
  CreateMatchChatRoomRequest,
  CreateMatchChatRoomResponse,
  MatchChatRoom
} from './types';

// 임시 목 데이터
const MOCK_ROOMS: MatchChatRoom[] = [
  {
    matchId: "match_game_20250806_001_345c0215",
    gameId: "game_20250806_001",
    matchTitle: "20대 LG팬들 모여라!",
    matchDescription: "20대 LG 트윈스 팬들끼리 채팅해요",
    teamId: "LG",
    minAge: 20,
    maxAge: 29,
    genderCondition: "ALL",
    maxParticipants: 10,
    currentParticipants: 3,
    createdAt: "2025-08-06T04:55:50.011618551",
    status: "ACTIVE",
    websocketUrl: "ws://10.0.2.2:8084/ws/match-chat/websocket?matchId=match_game_20250806_001_345c0215"
  },
  {
    matchId: "match_game_20250806_001_0e1b267d",
    gameId: null,
    matchTitle: "30대 두산팬 환영!",
    matchDescription: "30대 두산 베어스 팬들의 채팅방",
    teamId: "두산",
    minAge: 30,
    maxAge: 39,
    genderCondition: "MALE",
    maxParticipants: 8,
    currentParticipants: 5,
    createdAt: "2025-08-06T04:52:44.389962725",
    status: "ACTIVE",
    websocketUrl: "ws://10.0.2.2:8084/ws/match-chat/websocket?matchId=match_game_20250806_001_0e1b267d"
  }
];

// 실제 서버 응답을 처리하는 API 함수들 (서버 연결 실패시 목 데이터 사용)
export const chatRoomApi = {
  // 매치 채팅 참여 - 직접 타입 정의
  joinMatchChat: async (request: MatchChatJoinRequest): Promise<{ data: { status: string; message: string; data: AuthResponse } }> => {
    try {
      const response = await chatApiClient.post('/api/match-chat/join', request);
      return response as any;
    } catch (error) {
      console.warn('서버 연결 실패, 목 데이터 반환:', error);
      // 목 응답 반환
      return {
        data: {
          status: 'SUCCESS',
          message: '채팅방 참여 성공 (목 데이터)',
          data: {
            sessionToken: 'mock_session_token_' + Date.now(),
            websocketUrl: 'ws://10.0.2.2:8084/ws/match-chat/websocket?matchId=' + request.matchId
          }
        }
      };
    }
  },

  // 관전 채팅 참여
  joinWatchChat: async (request: WatchChatJoinRequest) => {
    try {
      const response = await chatApiClient.post('/api/watch-chat/join', request);
      return response as any;
    } catch (error) {
      console.warn('서버 연결 실패, 목 데이터 반환:', error);
      return {
        data: {
          status: 'SUCCESS',
          message: '워치 채팅 참여 성공 (목 데이터)',
          data: {
            sessionToken: 'mock_watch_session_' + Date.now(),
            websocketUrl: 'ws://10.0.2.2:8084/ws/watch-chat/websocket?gameId=' + request.gameId
          }
        }
      };
    }
  },

  // 세션 무효화
  invalidateSession: async (sessionToken: string) => {
    try {
      const response = await chatApiClient.delete(`/api/chat/auth/session/${sessionToken}`);
      return response as any;
    } catch (error) {
      console.warn('서버 연결 실패, 목 응답 반환:', error);
      return { data: { status: 'SUCCESS', message: '세션 무효화 완료 (목 데이터)' } };
    }
  },

  // 헬스체크
  healthCheck: async () => {
    try {
      const response = await chatApiClient.get('/api/chat/auth/health');
      return response as any;
    } catch (error) {
      console.warn('서버 연결 실패, 목 응답 반환:', error);
      return { data: { status: 'SUCCESS', message: 'Health OK (목 데이터)' } };
    }
  },

  // 매치 채팅방 목록 조회 - 직접 타입 정의
  getMatchChatRooms: async (): Promise<{ data: MatchChatRoomsResponse }> => {
    try {
      console.log('API 요청 시작: /api/match-chat-rooms');
      const response = await chatApiClient.get('/api/match-chat-rooms');
      console.log('API 응답 성공:', response.data);
      return response as any;
    } catch (error: any) {
      console.warn('API 요청 실패 - URL:', `${chatApiClient.defaults.baseURL}/api/match-chat-rooms`);
      console.warn('오류 상세:', error.response ? error.response.data : error.message);
      console.warn('오류 상태:', error.response ? error.response.status : 'Network Error');
      
      // 목 응답 반환
      return {
        data: {
          status: 'SUCCESS',
          message: '채팅방 목록 조회 성공 (목 데이터)',
          data: {
            rooms: MOCK_ROOMS,
            nextCursor: null,
            hasMore: false,
            count: MOCK_ROOMS.length
          }
        }
      };
    }
  },

  // 매치 채팅방 생성 - 직접 타입 정의
  createMatchChatRoom: async (request: CreateMatchChatRoomRequest): Promise<{ data: CreateMatchChatRoomResponse }> => {
    try {
      console.log('채팅방 생성 API 요청:', request);
      const response = await chatApiClient.post('/api/match-chat-rooms', request);
      console.log('채팅방 생성 API 응답 성공:', response.data);
      return response as any;
    } catch (error: any) {
      console.warn('채팅방 생성 API 실패:', error.response ? error.response.data : error.message);
      
      // 목 데이터 반환
      console.log('목 데이터로 채팅방 생성 처리');
      const newRoom: MatchChatRoom = {
        matchId: 'mock_match_' + Date.now(),
        gameId: request.gameId,
        matchTitle: request.matchTitle,
        matchDescription: request.matchDescription,
        teamId: request.teamId,
        minAge: request.minAge,
        maxAge: request.maxAge,
        genderCondition: request.genderCondition,
        maxParticipants: request.maxParticipants,
        currentParticipants: 0,
        createdAt: new Date().toISOString(),
        status: 'ACTIVE',
        websocketUrl: `ws://10.0.2.2:8084/ws/match-chat/websocket?matchId=mock_match_${Date.now()}`
      };
      
      return {
        data: {
          status: 'SUCCESS',
          message: '채팅방 생성 성공 (목 데이터)',
          data: newRoom
        }
      };
    }
  },

  // 서버 연결 테스트 함수 추가
  testConnection: async () => {
    try {
      console.log('서버 연결 테스트 시작...');
      const response = await chatApiClient.get('/health');
      console.log('서버 연결 성공:', response.data);
      return true;
    } catch (error: any) {
      console.warn('서버 연결 실패:', error.message);
      return false;
    }
  },
};
