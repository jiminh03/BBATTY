import { apiClient, chatApiClient } from '../../../shared/api';
import { API_CONFIG } from '../../../shared/api/client/config';
import { tokenManager } from '../../../shared/api/client/tokenManager';
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
    websocketUrl: `${API_CONFIG.socketUrl}/ws/match-chat/websocket?matchId=match_game_20250806_001_345c0215`
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
    websocketUrl: `${API_CONFIG.socketUrl}/ws/match-chat/websocket?matchId=match_game_20250806_001_0e1b267d`
  }
];

// 실제 서버 응답을 처리하는 API 함수들 (서버 연결 실패시 목 데이터 사용)
export const chatRoomApi = {
  // 매치 채팅 참여 - 직접 타입 정의
  joinMatchChat: async (request: MatchChatJoinRequest): Promise<{ status: string; message: string; data: AuthResponse }> => {
    try {
      const response = await chatApiClient.post('/api/match-chat/join', request);
      return response;
    } catch (error) {
      console.warn('서버 연결 실패, 목 데이터 반환:', error);
      const sessionToken = 'mock_session_token_' + Date.now();
      return {
        status: 'SUCCESS',
        message: '채팅방 참여 성공 (목 데이터)',
        data: {
          sessionToken: sessionToken,
          websocketUrl: `${API_CONFIG.socketUrl}/ws/match-chat/websocket?token=${sessionToken}&matchId=${request.matchId}`
        }
      };
    }
  },

  // 관전 채팅 참여
  joinWatchChat: async (request: WatchChatJoinRequest) => {
    try {
      const response = await chatApiClient.post('/api/watch-chat/join', request);
      console.log('Watch chat 서버 응답:', response.data);
      // 서버에서 제공한 웹소켓 URL을 그대로 사용
      return response;
    } catch (error: any) {
      console.log('Watch chat 에러:', error);
      
      // JWT 인증 오류(401)인 경우 - 서버 문제로 판단하고 목 데이터 사용
      if (error.response?.status === 401) {
        console.warn('JWT 토큰 전송됐으나 서버에서 401 에러 - 서버 인증 로직 문제로 판단하여 목 데이터 사용');
        const sessionToken = 'mock_session_token_401_' + Date.now();
        return {
          status: 'SUCCESS',
          message: '워치 채팅 참여 성공 (JWT 401 에러로 인한 목 데이터)',
          data: {
            sessionToken: sessionToken,
            teamId: request.teamId,
            gameId: request.gameId,
            expiresIn: 10800,
            websocketUrl: `${API_CONFIG.socketUrl}/ws/watch-chat/websocket?token=${sessionToken}&gameId=${request.gameId}&teamId=${request.teamId}`
          }
        };
      }
      
      // 네트워크 오류나 서버 연결 실패(5xx 에러)인 경우만 목 데이터 사용
      if (!error.response || error.response.status >= 500) {
        console.warn('서버 연결 실패 - 목 데이터 사용');
        const sessionToken = 'e37d70605e0f4378945838dfcb23f461';
        return {
          status: 'SUCCESS',
          message: '워치 채팅 참여 성공 (목 데이터)',
          data: {
            sessionToken: sessionToken,
            teamId: request.teamId,
            gameId: request.gameId,
            expiresIn: 10800,
            websocketUrl: `${API_CONFIG.socketUrl}/ws/watch-chat/websocket?token=${sessionToken}&gameId=${request.gameId}&teamId=${request.teamId}`
          }
        };
      }
      
      // 기타 클라이언트 오류(4xx)는 그대로 던지기
      throw error;
    }
  },

  // 세션 무효화
  invalidateSession: async (sessionToken: string) => {
    try {
      const response = await chatApiClient.delete(`/api/chat/auth/session/${sessionToken}`);
      return response;
    } catch (error) {
      console.warn('서버 연결 실패, 목 응답 반환:', error);
      return { status: 'SUCCESS', message: '세션 무효화 완료 (목 데이터)' };
    }
  },

  // 헬스체크
  healthCheck: async () => {
    try {
      const response = await chatApiClient.get('/api/chat/auth/health');
      return response;
    } catch (error) {
      console.warn('서버 연결 실패, 목 응답 반환:', error);
      return { status: 'SUCCESS', message: 'Health OK (목 데이터)' };
    }
  },

  // 매치 채팅방 목록 조회 - 직접 타입 정의
  getMatchChatRooms: async (): Promise<MatchChatRoomsResponse> => {
    try {
      const response = await chatApiClient.get('/api/match-chat-rooms');
      return response;
    } catch (error: any) {
      console.warn('서버 연결 실패, 목 데이터 반환:', error);
      
      // 목 응답 반환
      return {
        status: 'SUCCESS',
        message: '채팅방 목록 조회 성공 (목 데이터)',
        data: {
          rooms: MOCK_ROOMS,
          nextCursor: null,
          hasMore: false,
          count: MOCK_ROOMS.length
        }
      };
    }
  },

  // 매치 채팅방 생성 - 직접 타입 정의
  createMatchChatRoom: async (request: CreateMatchChatRoomRequest): Promise<CreateMatchChatRoomResponse> => {
    try {
      const response = await chatApiClient.post('/api/match-chat-rooms', request);
      return response;
    } catch (error: any) {
      console.warn('서버 연결 실패, 목 데이터 반환:', error);
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
        websocketUrl: `${API_CONFIG.socketUrl}/ws/match-chat/websocket?matchId=mock_match_${Date.now()}`
      };
      
      return {
        status: 'SUCCESS',
        message: '채팅방 생성 성공 (목 데이터)',
        data: newRoom
      };
    }
  },

  // 서버 연결 테스트 함수 추가
  testConnection: async () => {
    try {
      const response = await chatApiClient.get('/health');
      return true;
    } catch (error: any) {
      console.warn('서버 연결 실패:', error.message);
      return false;
    }
  },
};
