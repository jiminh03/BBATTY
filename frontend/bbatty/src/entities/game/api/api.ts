import { apiClient } from '../../../shared/api';
import type { GamesResponse, GameResponse, TodayGameResponse } from './types';

export const gameApi = {
  // 경기 목록 조회
  getGames: async (): Promise<GamesResponse> => {
    const response = await apiClient.get('/api/games/three-weeks');
    return response.data;
  },

  // 개별 게임 정보 조회
  getGameById: async (gameId: string | number): Promise<GameResponse> => {
    const response = await apiClient.get(`/api/games/${gameId}`);
    return response.data;
  },

  // 오늘의 게임 정보 조회
  getTodayGame: async (): Promise<TodayGameResponse> => {
    const response = await apiClient.get('/api/games/today');
    return response.data;
  },
};