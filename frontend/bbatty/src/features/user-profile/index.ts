// features/user-profile/index.ts

// API
export { profileApi } from './api/profileApi';
export { statsApi } from './api/statsApi';

// Models
export type {
  UserProfile,
  UserPrivacySettings,
  ProfileFormData,
  UpdateProfileRequest,
  CheckNicknameRequest,
  CheckNicknameResponse,
} from './model/profileTypes';

export type { UserBadges, BadgeCategory, Badge } from './model/badgeTypes';

export type {
  BasicStats,
  StreakStats,
  HomeAwayStats,
  StadiumStats,
  OpponentStats,
  DayOfWeekStats,
  TeamStats,
  StadiumStatsItem,
  DayOfWeekStatsItem,
  WinRateStats,
  DirectViewRecord,
  DetailedUserStats,
  StatsType,
} from './model/statsTypes';

// Hooks
export {
  useProfile,
  useUpdateProfile,
  useUpdatePrivacySettings,
  useUserBadges,
  useBasicStats,
  useDetailedStats,
  useHomeAwayStats,
  useStadiumStats,
  useOpponentStats,
  useDayOfWeekStats,
  useStreakStats,
  useAllUserStats,
} from './hooks/useProfile';

// Hooks
export { useDirectViewHistory, useUserStats } from './hooks/useUserStats';

export { useProfileForm } from './hooks/useProfileForm';

// UI Components
export { UserProfileHeader } from './ui/UserProfileHeader';
export { ProfileTabs } from './ui/ProfileTabs';
export { StatsTabHeader } from './ui/StatsTabHeader';
export { SeasonSelector } from './ui/SeasonSelector';
export { WinRateTypeSelector } from './ui/WinRateTypeSelector';
export type { WinRateType } from './ui/WinRateTypeSelector';
export { ProfileForm } from './ui/ProfileForm';
export { ProfileImagePicker } from './ui/ProfileImagePicker';
export { NicknameConflictModal } from './ui/NicknameConflictModal';
