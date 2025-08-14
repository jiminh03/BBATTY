import { apiClient } from '../../../shared/api';
import type { AttendanceVerificationRequest, AttendanceVerificationResponse } from './types';

export const attendanceApi = {
  // 직관 인증
  verifyAttendance: async (location: AttendanceVerificationRequest): Promise<AttendanceVerificationResponse> => {
    try {
      console.log('🎯 직관 인증 API 요청 시작:', location);
      const response = await apiClient.post('/api/attendance/verify', location);
      console.log('🎯 직관 인증 API 응답 성공:', response.data);
      return response.data;
    } catch (error: any) {
      // 400 에러는 비즈니스 로직 실패이므로 에러 로그 없이 응답만 반환
      if (error.response?.status === 400 && error.response?.data) {
        console.log('🎯 직관 인증 API 응답:', error.response.data);
        return error.response.data;
      }
      
      // 실제 네트워크 에러나 서버 에러만 에러 로그 출력
      console.error('🚨 직관 인증 API 요청 실패:', {
        message: error.message,
        code: error.code,
        status: error.response?.status,
        data: error.response?.data,
      });
      
      // 에러 응답이 있으면 그대로 반환
      if (error.response?.data) {
        return error.response.data;
      }
      
      // 네트워크 오류 등의 경우 예외를 다시 던져서 상위에서 처리하도록 함
      throw error;
    }
  },
};