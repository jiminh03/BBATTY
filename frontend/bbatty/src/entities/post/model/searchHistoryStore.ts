import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

type State = {
  byTeam: Record<number, string[]>;
  add: (teamId: number, q: string) => void;
  remove: (teamId: number, q: string) => void;
  clear: (teamId?: number) => void;
  getHistoryForTeam: (teamId: number) => string[];
};

const MAX = 10;

export const useSearchHistoryStore = create<State>()(
  persist(
    (set, get) => ({
      byTeam: {},
      getHistoryForTeam: (teamId) => {
        const state = get();
        return state.byTeam[teamId] ?? [];
      },
      add: (teamId, q) =>
        set((s) => {
          const cur = s.byTeam[teamId] ?? [];
          const nq = q.trim();
          if (!nq) return s;
          const arr = [nq, ...cur.filter((x) => x !== nq)].slice(0, MAX);
          return { byTeam: { ...s.byTeam, [teamId]: arr } };
        }),
      remove: (teamId, q) =>
        set((s) => {
          const cur = s.byTeam[teamId] ?? [];
          return { byTeam: { ...s.byTeam, [teamId]: cur.filter((x) => x !== q) } };
        }),
      clear: (teamId) =>
        set((s) =>
          teamId ? { byTeam: { ...s.byTeam, [teamId]: [] } } : { byTeam: {} }
        ),
    }),
    { name: 'post-search-history', storage: createJSONStorage(() => AsyncStorage) }
  )
);
