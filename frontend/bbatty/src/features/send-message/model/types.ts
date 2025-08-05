export interface SendMessageRequest {
  content: string;
  roomId: string;
  messageType?: 'CHAT' | 'SYSTEM';
  metadata?: Record<string, any>;
}

export interface SendMessageOptions {
  validateBeforeSend?: boolean;
  retryOnFailure?: boolean;
  maxRetries?: number;
  showSuccessToast?: boolean;
  clearAfterSend?: boolean;
}

export interface MessageValidation {
  isValid: boolean;
  errors: string[];
}

export interface RateLimitInfo {
  limit: number;
  remaining: number;
  resetAt: string;
  windowMs: number;
}