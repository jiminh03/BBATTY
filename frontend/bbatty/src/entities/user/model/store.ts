import { create } from 'zustand';
import { ChatUser, UserSessionInfo } from './types';

interface UserState {
  currentUser: ChatUser | null;
  sessionInfo: UserSessionInfo | null;
  connectedUsers: Record<string, ChatUser[]>; // roomId -> users
}

interface UserActions {
  setCurrentUser: (user: ChatUser | null) => void;
  setSessionInfo: (info: UserSessionInfo | null) => void;
  addUserToRoom: (roomId: string, user: ChatUser) => void;
  removeUserFromRoom: (roomId: string, userId: string) => void;
  setRoomUsers: (roomId: string, users: ChatUser[]) => void;
  clearRoomUsers: (roomId: string) => void;
  reset: () => void;
}

type UserStore = UserState & UserActions;

export const useUserStore = create<UserStore>((set, get) => ({
  // State
  currentUser: null,
  sessionInfo: null,
  connectedUsers: {},

  // Actions
  setCurrentUser: (user) => set({ currentUser: user }),

  setSessionInfo: (info) => set({ sessionInfo: info }),

  addUserToRoom: (roomId, user) =>
    set((state) => ({
      connectedUsers: {
        ...state.connectedUsers,
        [roomId]: [...(state.connectedUsers[roomId] || []), user],
      },
    })),

  removeUserFromRoom: (roomId, userId) =>
    set((state) => ({
      connectedUsers: {
        ...state.connectedUsers,
        [roomId]: (state.connectedUsers[roomId] || []).filter((u) => u.id !== userId),
      },
    })),

  setRoomUsers: (roomId, users) =>
    set((state) => ({
      connectedUsers: {
        ...state.connectedUsers,
        [roomId]: users,
      },
    })),

  clearRoomUsers: (roomId) =>
    set((state) => ({
      connectedUsers: {
        ...state.connectedUsers,
        [roomId]: [],
      },
    })),

  reset: () =>
    set({
      currentUser: null,
      sessionInfo: null,
      connectedUsers: {},
    }),
}));
