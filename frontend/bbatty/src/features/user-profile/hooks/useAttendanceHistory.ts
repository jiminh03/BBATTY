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
    const result = await statsApi.getAttendanceRecords({
      userId: params.userId,
      season: params.season,
      cursor: params.cursor as number | null,
      limit: params.limit || 20,
    });

    if (isOk(result)) {
      const response = result.data as AttendanceRecordsResponse;
      return {
        data: response.records,
        nextCursor: response.nextCursor,
        hasMore: response.hasMore,
      };
    }

    throw new Error(result.error.message);
  };

  return useInfiniteCursorScroll<AttendanceRecord, AttendanceHistoryParams>({
    queryKey: ['attendanceHistory', userId, season],
    apiFunction,
    initialParams: {
      userId,
      season,
      limit: 20,
    },
    staleTime: 5 * 60 * 1000, // 5분
  });
};