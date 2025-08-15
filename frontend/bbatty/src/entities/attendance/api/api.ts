import { apiClient } from '../../../shared/api';
import type { AttendanceVerificationRequest, AttendanceVerificationResponse } from './types';

export const attendanceApi = {
  // 직관 인증 - 단순한 API 호출로 원인 분석
  verifyAttendance: async (location: AttendanceVerificationRequest): Promise<AttendanceVerificationResponse> => {
    console.log('🎯 직관 인증 시작');
    console.log('📍 위치:', location);
    console.log('🌐 서버:', apiClient.defaults.baseURL);
    console.log('🔑 토큰 존재:', !!apiClient.defaults.headers.common['Authorization']);
    
    try {
      const response = await apiClient.post('/api/attendance/verify', location, {
        headers: {
          'Content-Type': 'application/json',
        },
        timeout: 10000
      });
      
      console.log('✅ 직관 인증 성공:', response.status);
      console.log('📦 응답 데이터:', response.data);
      return response.data;
      
    } catch (error: any) {
      console.log('❌ 직관 인증 실패 상세 분석:');
      console.log('- 에러 코드:', error.code);
      console.log('- 에러 메시지:', error.message);
      console.log('- 응답 상태:', error.response?.status);
      console.log('- 응답 데이터:', error.response?.data);
      console.log('- 요청 URL:', error.config?.url);
      console.log('- 베이스 URL:', error.config?.baseURL);
      console.log('- 요청 메소드:', error.config?.method);
      console.log('- 요청 헤더:', error.config?.headers);
      console.log('- 타임아웃:', error.config?.timeout);
      console.log('- 네트워크 에러 여부:', error.code === 'ERR_NETWORK');
      console.log('- 전체 에러 객체:', JSON.stringify(error, null, 2));
      
      // 서버 응답이 있으면 그대로 반환
      if (error.response?.data) {
        return error.response.data;
      }
      
      // 기본 에러 메시지
      return {
        status: 'ERROR',
        message: '서버와 연결할 수 없어요. 잠시 후 다시 시도해주세요.',
        data: null
      };
    }
  },
};