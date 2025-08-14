// useSearchHistoryStore.ts
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

type SearchHistoryState = {
  historiesByTeam: Record<number, string[]>;

  // new names
  getHistoryForTeam: (teamId: number) => string[];
  addHistory: (teamId: number, keyword: string) => void;
  removeHistory: (teamId: number, keyword: string) => void;
  clearHistory: (teamId?: number) => void;

  // backward-compatible aliases (OLD names)
  add: (teamId: number, keyword: string) => void;
  remove: (teamId: number, keyword: string) => void;
  clear: (teamId?: number) => void;
};

const MAX_HISTORY_LENGTH = 10;

export const useSearchHistoryStore = create<SearchHistoryState>()(
  persist(
    (set, get) => {
      const getHistoryForTeam = (teamId: number) =>
        get().historiesByTeam[teamId] ?? [];

      const addHistory = (teamId: number, keyword: string) =>
        set((state) => {
          const trimmed = keyword.trim();
          if (!trimmed) return state;

          const current = state.historiesByTeam[teamId] ?? [];
          const deduped = [trimmed, ...current.filter((k) => k !== trimmed)];
          const sliced = deduped.slice(0, MAX_HISTORY_LENGTH);

          return { historiesByTeam: { ...state.historiesByTeam, [teamId]: sliced } };
        });

      const removeHistory = (teamId: number, keyword: string) =>
        set((state) => {
          const current = state.historiesByTeam[teamId] ?? [];
          return {
            historiesByTeam: {
              ...state.historiesByTeam,
              [teamId]: current.filter((k) => k !== keyword),
            },
          };
        });

      const clearHistory = (teamId?: number) =>
        set((state) =>
          teamId
            ? { historiesByTeam: { ...state.historiesByTeam, [teamId]: [] } }
            : { historiesByTeam: {} },
        );

      return {
        historiesByTeam: {},

        // new
        getHistoryForTeam,
        addHistory,
        removeHistory,
        clearHistory,

        // aliases for old callers
        add: addHistory,
        remove: removeHistory,
        clear: clearHistory,
      };
    },
    {
      name: 'post-search-history',
      storage: createJSONStorage(() => AsyncStorage),
      // 과거 스냅샷과 합칠 때 함수는 유지되므로 OK
    },
  ),
);
