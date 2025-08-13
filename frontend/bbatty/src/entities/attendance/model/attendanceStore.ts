import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface AttendanceState {
  isAttendanceVerified: boolean;
  verifiedDate: string | null;
  todayGameInfo: {
    gameId: number;
    awayTeamName: string;
    homeTeamName: string;
    dateTime: string;
    stadium: string;
  } | null;
  setAttendanceVerified: (verified: boolean, gameInfo?: any) => void;
  clearAttendance: () => void;
  isVerifiedToday: () => boolean;
}

export const useAttendanceStore = create<AttendanceState>()(
  persist(
    (set, get) => ({
      isAttendanceVerified: false,
      verifiedDate: null,
      todayGameInfo: null,

      setAttendanceVerified: (verified, gameInfo) => {
        const today = new Date().toDateString();
        set({
          isAttendanceVerified: verified,
          verifiedDate: verified ? today : null,
          todayGameInfo: verified ? gameInfo : null,
        });
      },

      clearAttendance: () => {
        set({
          isAttendanceVerified: false,
          verifiedDate: null,
          todayGameInfo: null,
        });
      },

      isVerifiedToday: () => {
        const state = get();
        if (!state.isAttendanceVerified || !state.verifiedDate) {
          return false;
        }
        const today = new Date().toDateString();
        return state.verifiedDate === today;
      },
    }),
    {
      name: 'attendance-storage',
      storage: createJSONStorage(() => AsyncStorage),
    }
  )
);