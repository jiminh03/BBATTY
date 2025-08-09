import { apiClient } from '../../../shared/api';
import type { GamesResponse } from './types';

export const gameApi = {
  // 경기 목록 조회
  getGames: async (): Promise<GamesResponse> => {
    try {
      const response = await apiClient.get('/api/games/three-weeks');
      return response.data;
    } catch (error: any) {
      console.error('API 요청 실패 상세:', {
        message: error.message,
        status: error.response?.status,
        statusText: error.response?.statusText,
        headers: error.response?.headers,
        config: {
          url: error.config?.url,
          method: error.config?.method,
          headers: error.config?.headers
        }
      });
      console.warn('서버 연결 실패, 목 데이터 반환:', error);
      
      // 목 데이터 반환
      return {
        status: 'SUCCESS',
        message: '경기 목록 조회 성공 (목 데이터)',
        data: [
          {
            gameId: 1275,
            awayTeamName: "두산 베어스",
            homeTeamName: "키움 히어로즈",
            dateTime: "2025-08-10T14:00:00",
            stadium: "고척스카이돔"
          }
        ]
      };
    }
  },
};