// 앱 정보
export const APP_INFO = {
  NAME: '빠팅',
  VERSION: '1.0.0',
  BUILD_NUMBER: 1,
  BUNDLE_ID: 'com.ssafy.bbatty',
  DEEP_LINK_SCHEME: 'bbatty',
} as const;

// 스토리지 키
export const STORAGE_KEYS = {
  // 인증
  AUTH_TOKEN: 'auth_token',
  REFRESH_TOKEN: 'refresh_token',
  USER_ID: 'user_id',

  // 사용자 설정
  SELECTED_TEAM: 'selected_team',
  NOTIFICATION_ENABLED: 'notification_enabled',
  LOCATION_PERMISSION: 'location_permission',

  // 앱 상태
  FIRST_LAUNCH: 'first_launch',
  ONBOARDING_COMPLETED: 'onboarding_completed',
  APP_VERSION: 'app_version',

  // 캐시
  GAME_SCHEDULE_CACHE: 'game_schedule_cache',
  STADIUM_INFO_CACHE: 'stadium_info_cache',
} as const;

// 페이지네이션
export const PAGINATION = {
  DEFAULT_PAGE_SIZE: 20,
  MAX_PAGE_SIZE: 100,
  INITIAL_PAGE: 1,
} as const;

// 리프레시 간격 (밀리초)
export const REFRESH_INTERVALS = {
  GAME_STATUS: 30 * 1000, // 30초 (경기 중)
  CHAT_MESSAGES: 1000, // 1초
  USER_LOCATION: 10 * 1000, // 10초
  ATTENDANCE_CHECK: 5 * 60 * 1000, // 5분
} as const;

// 제한 사항
export const LIMITS = {
  // 텍스트 길이
  USERNAME_MIN: 2,
  USERNAME_MAX: 10,
  BIO_MAX: 100,
  CHAT_MESSAGE_MAX: 500,
  POST_TITLE_MAX: 100,
  POST_CONTENT_MAX: 5000,
  COMMENT_MAX: 1000,

  // 파일
  IMAGE_MAX_SIZE: 10 * 1024 * 1024, // 10MB
  IMAGE_MAX_COUNT: 10,

  // 기타
  REPORT_REASON_MAX: 500,
  SEARCH_QUERY_MIN: 2,
  SEARCH_QUERY_MAX: 50,
} as const;

// 에러 메시지
export const ERROR_MESSAGES = {
  NETWORK: '네트워크 연결을 확인해주세요',
  SERVER: '서버 오류가 발생했습니다',
  UNAUTHORIZED: '로그인이 필요합니다',
  FORBIDDEN: '권한이 없습니다',
  NOT_FOUND: '요청한 정보를 찾을 수 없습니다',
  VALIDATION: '입력 정보를 확인해주세요',
  FILE_TOO_LARGE: '파일 크기가 너무 큽니다',
  LOCATION_PERMISSION: '위치 권한이 필요합니다',
  CAMERA_PERMISSION: '카메라 권한이 필요합니다',
} as const;

// 성공 메시지
export const SUCCESS_MESSAGES = {
  SAVE: '저장되었습니다',
  DELETE: '삭제되었습니다',
  UPDATE: '수정되었습니다',
  ATTENDANCE_VERIFIED: '직관 인증이 완료되었습니다',
} as const;

// 채팅방 타입
export const CHAT_ROOM_TYPES = {
  GAME: 'GAME', // 직관 채팅방
  MATCH: 'MATCH', // 매칭 채팅방
} as const;

// 알림 타입
export const NOTIFICATION_TYPES = {
  CHAT_MESSAGE: 'CHAT_MESSAGE',
  MATCH_REQUEST: 'MATCH_REQUEST',
  MATCH_ACCEPTED: 'MATCH_ACCEPTED',
  GAME_REMINDER: 'GAME_REMINDER',
  ATTENDANCE_VERIFIED: 'ATTENDANCE_VERIFIED',
  POST_COMMENT: 'POST_COMMENT',
  POST_LIKE: 'POST_LIKE',
} as const;

// 딥링크 경로
export const DEEP_LINK_PATHS = {
  GAME: 'game',
  CHAT: 'chat',
  POST: 'post',
  PROFILE: 'profile',
  SETTINGS: 'settings',
} as const;
