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
