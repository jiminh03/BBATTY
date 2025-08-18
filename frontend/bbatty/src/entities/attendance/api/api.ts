import { apiClient } from '../../../shared/api';
import type { AttendanceVerificationRequest, AttendanceVerificationResponse } from './types';

export const attendanceApi = {
  // 직관 인증 - 단순한 API 호출로 원인 분석
  verifyAttendance: async (location: AttendanceVerificationRequest): Promise<AttendanceVerificationResponse> => {
    try {
      const response = await apiClient.post('/api/attendance/verify', location, {
        headers: {
          'Content-Type': 'application/json',
        },
        timeout: 10000,
      });
      return response.data;
    } catch (error: any) {
      // 서버 응답이 있으면 그대로 반환
      if (error.response?.data) {
        return error.response.data;
      }

      // 기본 에러 메시지
      return {
        status: 'ERROR',
        message: '서버와 연결할 수 없어요. 잠시 후 다시 시도해주세요.',
        data: null,
      };
    }
  },
};
