import { apiClient } from '../../../shared/api';
import type { AttendanceVerificationRequest, AttendanceVerificationResponse } from './types';

export const attendanceApi = {
  // 직관 인증 - 서버 응답 메시지만 사용하는 단순한 구현
  verifyAttendance: async (location: AttendanceVerificationRequest): Promise<AttendanceVerificationResponse> => {
    console.log('🎯 직관 인증 API 요청 시작:', location);
    console.log('🔍 요청 세부사항:', {
      url: '/api/attendance/verify',
      method: 'POST',
      body: location,
      timestamp: new Date().toISOString(),
      headers: {
        'X-Skip-Error-Toast': 'true',
        // Authorization은 interceptor에서 자동 추가됨
      }
    });
    
    try {
      // 성공한 경우
      const response = await apiClient.post('/api/attendance/verify', location, {
        headers: {
          'X-Skip-Error-Toast': 'true',
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      });
      
      console.log('✅ 직관 인증 성공:', response.data);
      return response.data;
      
    } catch (error: any) {
      console.log('❌ 직관 인증 실패 - 전체 에러 정보:', {
        message: error.message,
        code: error.code,
        status: error.response?.status,
        hasResponse: !!error.response,
        hasRequest: !!error.request
      });
      
      // 1. error.response.data가 있으면 (가장 일반적인 경우)
      if (error.response?.data) {
        console.log('📨 서버 응답 데이터 (HTTP ' + error.response.status + '):', error.response.data);
        return error.response.data;
      }
      
      // 2. React Native에서 responseText 파싱 시도
      if (error.request?.responseText) {
        try {
          const parsedResponse = JSON.parse(error.request.responseText);
          console.log('📨 파싱된 서버 응답:', parsedResponse);
          return parsedResponse;
        } catch (parseError) {
          console.log('⚠️ 응답 파싱 실패');
        }
      }
      
      // 3. 모든 방법이 실패한 경우에만 기본 메시지
      console.log('🚨 서버 응답을 읽을 수 없음 - 기본 메시지 사용');
      return {
        status: 'ERROR',
        message: '서버와 연결할 수 없어요. 잠시 후 다시 시도해주세요.',
        data: null
      };
    }
  },
};