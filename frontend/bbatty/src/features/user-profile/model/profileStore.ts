// features/user-profile/model/profileStore.ts
import { create } from 'zustand';
import { useUserStore } from '../../../entities/user/model/userStore';
import { UserProfile, UserPrivacySettings } from './profileTypes';
import { profileApi } from '../api/profileApi';
import { isOk } from '../../../shared/utils/result';
import { TabType } from '../ui/ProfileTabs';
import { WinRateType } from '../ui/WinRateTypeSelector';
import { Season } from '../../../shared/utils/date';

interface ProfileState {
  currentProfile: UserProfile | null;
  isProfileLoading: boolean;
  profileError: string | null;
  // Tab persistence state
  activeTab: TabType;
  winRateType: WinRateType;
  selectedSeason: Season;
  scrollPositions: { [key in TabType]?: number };
}

interface ProfileActions {
  loadProfile: (userId?: number) => Promise<void>;
  updatePrivacySettings: (settings: UserPrivacySettings) => Promise<boolean>;
  clearProfile: () => void;
  // Tab persistence actions
  setActiveTab: (tab: TabType) => void;
  setWinRateType: (type: WinRateType) => void;
  setSelectedSeason: (season: Season) => void;
  setScrollPosition: (tab: TabType, position: number) => void;
  resetTabState: () => void;
}

type ProfileStore = ProfileState & ProfileActions;

export const useProfileStore = create<ProfileStore>((set, get) => ({
  currentProfile: null,
  isProfileLoading: false,
  profileError: null,
  // Tab persistence initial state
  activeTab: 'posts',
  winRateType: 'summary',
  selectedSeason: 'total',
  scrollPositions: {},

  loadProfile: async (userId?: number) => {
    set({ isProfileLoading: true, profileError: null });

    try {
      const result = await profileApi.getProfile(userId);

      if (isOk(result)) {
        set({
          currentProfile: result.data,
          isProfileLoading: false,
          profileError: null,
        });

        // 본인 프로필인 경우 userStore도 업데이트
        if (!userId) {
          const { setCurrentUser } = useUserStore.getState();
          await setCurrentUser(result.data);
        }
      } else {
        set({
          profileError: result.error.message,
          isProfileLoading: false,
        });
      }
    } catch (error) {
      set({
        profileError: error instanceof Error ? error.message : '프로필 로드 실패',
        isProfileLoading: false,
      });
    }
  },

  updatePrivacySettings: async (settings: UserPrivacySettings) => {
    const currentProfile = get().currentProfile;
    if (!currentProfile) return false;

    try {
      const result = await profileApi.updatePrivacySettings(settings);

      if (isOk(result)) {
        // 로컬 상태 업데이트
        const updatedProfile = {
          ...currentProfile,
          ...settings,
        };

        set({ currentProfile: updatedProfile });

        // userStore도 업데이트 (본인 프로필인 경우)
        const { setCurrentUser } = useUserStore.getState();
        await setCurrentUser(updatedProfile);

        return true;
      }
      return false;
    } catch (error) {
      return false;
    }
  },

  clearProfile: () => {
    set({
      currentProfile: null,
      isProfileLoading: false,
      profileError: null,
    });
  },

  // Tab persistence actions
  setActiveTab: (tab: TabType) => {
    set({ activeTab: tab });
  },

  setWinRateType: (type: WinRateType) => {
    set({ winRateType: type });
  },

  setSelectedSeason: (season: Season) => {
    set({ selectedSeason: season });
  },

  setScrollPosition: (tab: TabType, position: number) => {
    set((state) => ({
      scrollPositions: {
        ...state.scrollPositions,
        [tab]: position,
      },
    }));
  },

  resetTabState: () => {
    set({
      activeTab: 'posts',
      winRateType: 'summary',
      selectedSeason: 'total',
      scrollPositions: {},
    });
  },
}));
