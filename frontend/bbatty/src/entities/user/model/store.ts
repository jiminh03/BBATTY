import { create } from 'zustand';
import { User } from './types';

interface UserStore {
  currentUser: User | null;
  connectedUsers: Record<string, User[]>; // roomId별로 연결된 사용자들
  setCurrentUser: (user: User) => void;
  addConnectedUser: (roomId: string, user: User) => void;
  removeConnectedUser: (roomId: string, userId: string) => void;
  clearConnectedUsers: (roomId: string) => void;
  getConnectedUsers: (roomId: string) => User[];
}

export const useUserStore = create<UserStore>((set, get) => ({
  currentUser: null,
  connectedUsers: {},
  
  setCurrentUser: (user: User) => {
    set({ currentUser: user });
  },
  
  addConnectedUser: (roomId: string, user: User) => {
    set((state) => ({
      connectedUsers: {
        ...state.connectedUsers,
        [roomId]: [...(state.connectedUsers[roomId] || []), user]
      }
    }));
  },
  
  removeConnectedUser: (roomId: string, userId: string) => {
    set((state) => ({
      connectedUsers: {
        ...state.connectedUsers,
        [roomId]: (state.connectedUsers[roomId] || []).filter(user => user.userId !== userId)
      }
    }));
  },
  
  clearConnectedUsers: (roomId: string) => {
    set((state) => ({
      connectedUsers: {
        ...state.connectedUsers,
        [roomId]: []
      }
    }));
  },
  
  getConnectedUsers: (roomId: string) => {
    return get().connectedUsers[roomId] || [];
  }
}));