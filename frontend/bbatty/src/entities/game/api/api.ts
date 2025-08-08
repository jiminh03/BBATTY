import { chatApiClient } from '../../../shared/api';
import type { GamesResponse } from './types';

export const gameApi = {
  // 경기 목록 조회
  getGames: async (): Promise<GamesResponse> => {
    try {
      const response = await chatApiClient.get('/api/match/games');
      return response;
    } catch (error: any) {
      console.warn('서버 연결 실패, 목 데이터 반환:', error);
      
      // 목 데이터 반환
      return {
        status: 'SUCCESS',
        message: '경기 목록 조회 성공 (목 데이터)',
        data: [
            {
              date: "2025-08-07",
              games: [
                {
                  gameId: 1,
                  awayTeamId: 7,
                  homeTeamId: 10,
                  awayTeamName: "한화 이글스",
                  homeTeamName: "KIA 타이거즈",
                  status: "LIVE",
                  dateTime: "2025-08-07T19:30:00",
                  stadium: "광주기아챔피언스필드",
                  activeUserCount: 125
                },
                {
                  gameId: 2,
                  awayTeamId: 1,
                  homeTeamId: 3,
                  awayTeamName: "LG 트윈스",
                  homeTeamName: "두산 베어스",
                  status: "SCHEDULED",
                  dateTime: "2025-08-07T18:30:00",
                  stadium: "잠실야구장",
                  activeUserCount: 89
                },
                {
                  gameId: 3,
                  awayTeamId: 4,
                  homeTeamId: 6,
                  awayTeamName: "삼성 라이온즈",
                  homeTeamName: "NC 다이노스",
                  status: "SCHEDULED",
                  dateTime: "2025-08-07T18:30:00",
                  stadium: "창원NC파크",
                  activeUserCount: 67
                }
              ]
            },
            {
              date: "2025-08-08",
              games: [
                {
                  gameId: 4,
                  awayTeamId: 8,
                  homeTeamId: 9,
                  awayTeamName: "롯데 자이언츠",
                  homeTeamName: "KT 위즈",
                  status: "SCHEDULED",
                  dateTime: "2025-08-08T19:00:00",
                  stadium: "수원KT위즈파크",
                  activeUserCount: 0
                }
              ]
            }
          ]
      };
    }
  },
};