import { useInfiniteCursorScroll } from '../../../shared/api/hooks/useInfiniteCursorScroll';
import { statsApi } from '../api/statsApi';
import { AttendanceRecord, AttendanceRecordsResponse } from '../model/statsTypes';
import { Season } from '../../../shared/utils/date';
import { isOk } from '../../../shared/utils/result';
import type { CursorScrollParams, CursorScrollResponse } from '../../../shared/api/types/cursorScroll';

interface AttendanceHistoryParams extends CursorScrollParams {
  userId?: number;
  season?: Season;
}

export const useAttendanceHistory = (userId?: number, season: Season = 'total') => {
  const apiFunction = async (params: AttendanceHistoryParams): Promise<CursorScrollResponse<AttendanceRecord>> => {
    console.log('📊 [AttendanceHistory] API 호출 시작:', {
      userId: params.userId,
      season: params.season,
      cursor: params.cursor,
      limit: params.limit || 10,
    });

    const result = await statsApi.getAttendanceRecords({
      userId: params.userId,
      season: params.season,
      cursor: params.cursor as number | null,
      limit: params.limit || 10,
    });

    if (isOk(result)) {
      const response = result.data as AttendanceRecordsResponse;

      console.log('✅ [AttendanceHistory] API 응답 성공:', {
        recordsCount: response.records.length,
        nextCursor: response.nextCursor,
        hasMore: response.hasMore,
        firstRecord: response.records[0]
          ? {
              gameId: response.records[0].gameId,
              teams: `${response.records[0].homeTeam} vs ${response.records[0].awayTeam}`,
              date: response.records[0].dateTime,
              homeScore: response.records[0].homeScore,
              awayScore: response.records[0].awayScore,
            }
          : null,
      });

      return {
        data: response.records,
        nextCursor: response.nextCursor,
        hasMore: response.hasMore,
      };
    }

    console.error('❌ [AttendanceHistory] API 응답 실패:', result.error);
    throw new Error(result.error.message);
  };

  return useInfiniteCursorScroll<AttendanceRecord, AttendanceHistoryParams>({
    queryKey: ['attendanceHistory', userId, season],
    apiFunction,
    initialParams: {
      userId,
      season,
      limit: 10,
    },
    staleTime: 5 * 60 * 1000, // 5분
  });
};
