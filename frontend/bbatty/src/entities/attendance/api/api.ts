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
      
      // 에러 응답이 있으면 그대로 반환
      if (error.response?.data) {
        console.log('🎯 직관 인증 API 응답:', error.response.data);
        return error.response.data;
      }
      
      // error.response가 없지만 request.status가 4xx인 경우 (React Native 환경에서 발생)
      const requestStatus = error.request?.status;
      if (requestStatus && requestStatus >= 400 && requestStatus < 500 && !error.response) {
        console.log(`📋 ${requestStatus} 에러 - response 파싱 시도`);
        
        // XMLHttpRequest의 responseText에서 실제 응답 데이터 추출 시도
        let responseData = null;
        try {
          if (error.request.responseText) {
            responseData = JSON.parse(error.request.responseText);
          }
        } catch (parseError) {
          // 파싱 실패는 정상적인 상황이므로 로깅하지 않음
        }
        
        // 파싱된 데이터가 있으면 사용, 없으면 상태코드별 기본 메시지
        if (responseData) {
          console.log('🎯 직관 인증 API 응답:', responseData);
          return responseData;
        }
        
        // 파싱 실패 시 상태코드별 기본 메시지
        const defaultMessages = {
          400: '경기 시간에만 인증할 수 있어요.',
          404: '오늘은 우리 팀 경기가 없어요.',
          409: '이미 해당 경기에 대해 직관 인증이 완료되었어요.',
        };
        
        const response = {
          status: 'ERROR',
          message: defaultMessages[requestStatus as keyof typeof defaultMessages] || '인증에 실패했습니다.',
          data: null
        };
        console.log('🎯 직관 인증 API 응답:', response);
        return response;
      }
      
      // 여기까지 왔다면 실제 네트워크 오류나 예상치 못한 에러
      console.error('🚨 직관 인증 API 요청 실패:', {
        message: error.message,
        code: error.code,
        status: error.response?.status,
      });
      
      // 네트워크 오류 등의 경우 예외를 다시 던져서 상위에서 처리하도록 함
      throw error;
    }
  },
};