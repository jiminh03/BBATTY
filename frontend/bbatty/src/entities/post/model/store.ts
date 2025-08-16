// entities/post/model/store.ts
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

/**
 * ✅ 호환 스토어
 * - 새 구조:   likedByUser[userId][postId] = boolean
 * - 레거시:    byPostId / byPostCount / ts 유지(읽는 쪽 호환)
 * - setSessionUser 호출 시, 레거시 byPostId를 현재 유저 상태로 동기화
 */
type LikeStore = {
  // --- 새 구조 ---
  sessionUserId: number | null;
  likedByUser: Record<number, Record<number, boolean>>;

  // --- 레거시 호환 필드(읽는 곳에서 에러 안 나게) ---
  byPostId: Record<number, boolean | undefined>;
  byPostCount: Record<number, number | undefined>;
  ts: Record<number, number | undefined>;

  // --- API ---
  setSessionUser: (userId: number | null) => void;

  getLiked: (postId: number, userId?: number | null) => boolean | undefined;
  setLiked: (postId: number, liked: boolean, userId?: number | null) => void;

  getCount: (postId: number) => number | undefined;         // 레거시용(현재는 사용 안 함)
  setCount: (postId: number, count?: number) => void;       // 레거시용 no-op 성격

  clearForUser: (userId: number | null) => void;
  clearAll: () => void;
};

export const useLikeStore = create<LikeStore>()(
  persist(
    (set, get) => ({
      // 초기값을 전부 {} 로 깔아두면 Object.keys/스프레드 시 크래시 없음
      sessionUserId: null,
      likedByUser: {},

      byPostId: {},
      byPostCount: {},
      ts: {},

      setSessionUser: (userId) =>
        set((s) => {
          const uid = userId ?? null;
          // 현재 유저의 liked 맵
          const mapForUser = (uid != null ? s.likedByUser[uid] : undefined) ?? {};
          // 레거시 필드에 현재 유저 맵을 투영
          return {
            sessionUserId: uid,
            byPostId: { ...mapForUser },
          };
        }),

      getLiked: (postId, userId) => {
        const uid = (userId ?? get().sessionUserId) ?? -1;
        const liked = get().likedByUser[uid]?.[postId];
        // 레거시(byPostId) fallback도 제공(혹시 초기 세션 설정 전이라면)
        return typeof liked === 'boolean' ? liked : get().byPostId[postId];
      },

      setLiked: (postId, liked, userId) =>
        set((s) => {
          const uid = (userId ?? s.sessionUserId) ?? -1;
          const cur = s.likedByUser[uid] ?? {};
          const nextForUser = { ...cur, [postId]: liked };

          // 레거시(byPostId/ts)도 같이 갱신(호환)
          const isCurrentUser = uid === (s.sessionUserId ?? -1);
          return {
            likedByUser: { ...s.likedByUser, [uid]: nextForUser },
            byPostId: isCurrentUser ? { ...s.byPostId, [postId]: liked } : s.byPostId,
            ts: isCurrentUser ? { ...s.ts, [postId]: Date.now() } : s.ts,
          };
        }),

      getCount: (postId) => get().byPostCount[postId],
      setCount: (postId, count) =>
        set((s) => ({
          // 현재는 카운트를 스토어에 의존하지 않지만,
          // 레거시 경로에서 호출될 수 있으므로 안전하게 반영만 해 둠
          byPostCount: { ...s.byPostCount, [postId]: count },
          ts: { ...s.ts, [postId]: Date.now() },
        })),

      clearForUser: (userId) =>
        set((s) => {
          const uid = (userId ?? s.sessionUserId) ?? -1;
          const next = { ...s.likedByUser };
          delete next[uid];

          // 현재 세션을 지우는 경우 레거시도 초기화
          const clearLegacy = uid === (s.sessionUserId ?? -1);
          return {
            likedByUser: next,
            ...(clearLegacy ? { byPostId: {}, byPostCount: {}, ts: {} } : {}),
          };
        }),

      clearAll: () => set({ likedByUser: {}, byPostId: {}, byPostCount: {}, ts: {} }),
    }),
    {
      name: 'likeStore-v2',               // v2로 분리(예전 잘못된 저장분과 충돌 방지)
      version: 2,
      storage: createJSONStorage(() => AsyncStorage),
      migrate: (persisted: any, ver) => {
        // 안전 마이그레이션: 빠진 필드 채워넣기
        const base = persisted ?? {};
        return {
          sessionUserId: base.sessionUserId ?? null,
          likedByUser: base.likedByUser ?? {},
          byPostId: base.byPostId ?? {},
          byPostCount: base.byPostCount ?? {},
          ts: base.ts ?? {},
        } as LikeStore;
      },
    }
  )
);
