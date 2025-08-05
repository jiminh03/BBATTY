import { create } from 'zustand';

interface SendMessageState {
  message: string;
  isLoading: boolean;
  error: string | null;
  lastSentMessage: string | null;
  lastSentAt: string | null;
  sendCount: number;
  rateLimitReached: boolean;
  rateLimitResetAt: string | null;
}

interface SendMessageActions {
  setMessage: (message: string) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  setSentMessage: (message: string) => void;
  incrementSendCount: () => void;
  setRateLimit: (reached: boolean, resetAt?: string) => void;
  clearMessage: () => void;
  reset: () => void;
}

type SendMessageStore = SendMessageState & SendMessageActions;

export const useSendMessageStore = create<SendMessageStore>((set, get) => ({
  // State
  message: '',
  isLoading: false,
  error: null,
  lastSentMessage: null,
  lastSentAt: null,
  sendCount: 0,
  rateLimitReached: false,
  rateLimitResetAt: null,

  // Actions
  setMessage: (message) => set({ message, error: null }),
  setLoading: (loading) => set({ isLoading: loading }),
  setError: (error) => set({ error }),
  setSentMessage: (message) => set({ 
    lastSentMessage: message, 
    lastSentAt: new Date().toISOString() 
  }),
  incrementSendCount: () => set((state) => ({ 
    sendCount: state.sendCount + 1 
  })),
  setRateLimit: (reached, resetAt) => set({ 
    rateLimitReached: reached, 
    rateLimitResetAt: resetAt 
  }),
  clearMessage: () => set({ message: '', error: null }),
  reset: () => set({
    message: '',
    isLoading: false,
    error: null,
    lastSentMessage: null,
    lastSentAt: null,
    sendCount: 0,
    rateLimitReached: false,
    rateLimitResetAt: null,
  }),
}));