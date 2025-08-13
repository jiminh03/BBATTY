import { apiClient } from '../../../shared/api';
import type { GamesResponse } from './types';

export const gameApi = {
  // 경기 목록 조회
  getGames: async (): Promise<GamesResponse> => {
    try {
      console.log('🎯 API 요청 시작: /api/games/three-weeks');
      const response = await apiClient.get('/api/games/three-weeks');
      console.log('🎯 API 응답 성공:', response.data);
      return response.data;
    } catch (error: any) {
      console.error('🚨 API 요청 실패 상세:', {
        message: error.message,
        code: error.code,
        status: error.response?.status,
        data: error.response?.data,
      });
      
      // 목 데이터 반환
      console.log('📦 목 데이터 반환 중...');
      return {
        status: 'SUCCESS',
        message: '경기 목록 조회 성공 (목 데이터)',
        data: [
          {
            gameId: 1309,
            awayTeamName: "롯데 자이언츠",
            homeTeamName: "한화 이글스",
            dateTime: "2025-08-14T18:30:00",
            stadium: "대전한화생명볼파크"
          },
          {
            gameId: 1310,
            awayTeamName: "두산 베어스",
            homeTeamName: "키움 히어로즈",
            dateTime: "2025-08-15T18:30:00",
            stadium: "고척스카이돔"
          }
        ]
      };
    }
  },
};