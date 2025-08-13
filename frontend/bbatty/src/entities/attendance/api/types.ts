export interface AttendanceVerificationRequest {
  latitude: number;
  longitude: number;
}

export interface AttendanceVerificationResponse {
  status: 'SUCCESS' | 'ERROR';
  message: string;
  data: any;
}