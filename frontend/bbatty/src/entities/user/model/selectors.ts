// entities/user/model/selectors.ts
export type AnyUser = {
  id?: number;
  userId?: number;
  uid?: number;
  kakaoUserId?: number;
  teamId?: number;
  nickname?: string;
};

export const selectUserId = (s: { currentUser?: AnyUser | null }) => {
  const u = s.currentUser;
  return (u?.userId ?? u?.id ?? u?.uid ?? u?.kakaoUserId ?? null) as number | null;
};

export const selectTeamId = (s: { currentUser?: AnyUser | null }) => {
  const u = s.currentUser;
  return (u?.teamId ?? null) as number | null;
};

export const selectNickname = (s: { currentUser?: AnyUser | null }) => {
  return (s.currentUser?.nickname ?? null) as string | null;
};
