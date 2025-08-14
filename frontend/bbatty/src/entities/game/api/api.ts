import { apiClient } from '../../../shared/api';
import type { GamesResponse, GameResponse, TodayGameResponse } from './types';

export const gameApi = {
  // 경기 목록 조회
  getGames: async (): Promise<GamesResponse> => {
    try {
      const response = await apiClient.get('/api/games/three-weeks');
      return response.data;
    } catch (error: any) {
      // 목 데이터 반환
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

  // 개별 게임 정보 조회
  getGameById: async (gameId: string | number): Promise<GameResponse> => {
    try {
      const response = await apiClient.get(`/api/games/${gameId}`);
      return response.data;
    } catch (error: any) {
      // 목 데이터 반환
      return {
        status: 'SUCCESS',
        message: '게임 정보 조회 성공 (목 데이터)',
        data: {
          gameId: Number(gameId),
          awayTeamName: "한화 이글스",
          homeTeamName: "LG 트윈스",
          dateTime: "2025-08-08T18:30:00",
          stadium: "잠실야구장"
        }
      };
    }
  },

  // 오늘의 게임 정보 조회
  getTodayGame: async (): Promise<TodayGameResponse> => {
    try {
      const response = await apiClient.get('/api/games/today');
      return response.data;
    } catch (error: any) {
      // 목 데이터 반환
      return {
        status: 'SUCCESS',
        message: '오늘 게임 정보 조회 성공 (목 데이터)',
        data: {
          gameId: 1303,
          awayTeamName: "NC 다이노스",
          homeTeamName: "두산 베어스",
          dateTime: "2025-08-13T18:30:00",
          stadium: "잠실야구장"
        }
      };
    }
  },
};