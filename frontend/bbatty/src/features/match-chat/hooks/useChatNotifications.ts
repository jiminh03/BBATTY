import { useState, useCallback, useRef, useEffect } from 'react';
import type { ChatNotification, ExtendedConnectionStatus } from '../types';
import type { ChatError } from '../../../shared/utils/error';

export const useChatNotifications = () => {
  const [notifications, setNotifications] = useState<ChatNotification[]>([]);
  const notificationTimeouts = useRef<Map<string, NodeJS.Timeout>>(new Map());

  // 알림 추가
  const addNotification = useCallback((notification: Omit<ChatNotification, 'id'>) => {
    const id = `notification_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const newNotification: ChatNotification = {
      ...notification,
      id,
      duration: notification.duration ?? 5000, // 기본 5초
    };

    setNotifications(prev => [...prev, newNotification]);

    // 자동 제거 타이머
    if (newNotification.duration && newNotification.duration > 0) {
      const timeoutId = setTimeout(() => {
        dismissNotification(id);
      }, newNotification.duration);

      notificationTimeouts.current.set(id, timeoutId);
    }

    return id;
  }, []);

  // 알림 제거
  const dismissNotification = useCallback((id: string) => {
    // 타이머 정리
    const timeoutId = notificationTimeouts.current.get(id);
    if (timeoutId) {
      clearTimeout(timeoutId);
      notificationTimeouts.current.delete(id);
    }

    setNotifications(prev => prev.filter(n => n.id !== id));
  }, []);

  // 모든 알림 제거
  const clearAllNotifications = useCallback(() => {
    // 모든 타이머 정리
    notificationTimeouts.current.forEach(timeoutId => clearTimeout(timeoutId));
    notificationTimeouts.current.clear();
    
    setNotifications([]);
  }, []);

  // 특정 타입의 알림만 제거
  const dismissNotificationType = useCallback((type: ChatNotification['type']) => {
    const typeNotifications = notifications.filter(n => n.type === type);
    typeNotifications.forEach(n => dismissNotification(n.id));
  }, [notifications, dismissNotification]);

  // 연결 상태 기반 알림
  const showConnectionNotification = useCallback((
    status: ExtendedConnectionStatus,
    reconnectAttempts?: number,
    maxAttempts?: number
  ) => {
    // 기존 연결 관련 알림들 제거
    const existingConnectionNotifications = notifications.filter(n => 
      n.message.includes('연결') || n.message.includes('오프라인') || n.message.includes('재연결')
    );
    existingConnectionNotifications.forEach(n => dismissNotification(n.id));

    switch (status) {
      case 'CONNECTED':
        addNotification({
          type: 'success',
          message: '채팅 서버에 연결되었습니다',
          duration: 3000,
        });
        break;
      case 'RECONNECTING':
        addNotification({
          type: 'warning',
          message: `재연결 중입니다... (${reconnectAttempts}/${maxAttempts})`,
          duration: 0, // 연결될 때까지 표시
        });
        break;
      case 'OFFLINE':
        addNotification({
          type: 'warning',
          message: '인터넷 연결을 확인해 주세요',
          duration: 0, // 연결될 때까지 표시
        });
        break;
      case 'ERROR':
        addNotification({
          type: 'error',
          message: '채팅 서버 연결에 실패했습니다',
          duration: 0,
          action: {
            label: '재시도',
            onPress: () => {
              // 재연결 시도는 외부에서 처리
            }
          }
        });
        break;
    }
  }, [notifications, addNotification, dismissNotification]);

  // 에러 기반 알림
  const showErrorNotification = useCallback((error: ChatError, onRetry?: () => void) => {
    const notificationConfig = {
      type: getNotificationTypeFromSeverity(error.severity),
      message: error.userMessage,
      duration: error.severity === 'critical' ? 0 : 8000,
    } as const;

    if (error.retryable && onRetry) {
      addNotification({
        ...notificationConfig,
        action: {
          label: '재시도',
          onPress: onRetry,
        },
      });
    } else {
      addNotification(notificationConfig);
    }
  }, [addNotification]);

  // 메시지 전송 관련 알림
  const showMessageNotification = useCallback((type: 'success' | 'failed', count = 1) => {
    switch (type) {
      case 'success':
        if (count > 1) {
          addNotification({
            type: 'success',
            message: `${count}개의 메시지가 전송되었습니다`,
            duration: 2000,
          });
        }
        break;
      case 'failed':
        addNotification({
          type: 'error',
          message: count > 1 
            ? `${count}개의 메시지 전송에 실패했습니다` 
            : '메시지 전송에 실패했습니다',
          duration: 5000,
        });
        break;
    }
  }, [addNotification]);

  // 정리 작업
  useEffect(() => {
    return () => {
      notificationTimeouts.current.forEach(timeoutId => clearTimeout(timeoutId));
      notificationTimeouts.current.clear();
    };
  }, []);

  return {
    notifications,
    addNotification,
    dismissNotification,
    clearAllNotifications,
    dismissNotificationType,
    showConnectionNotification,
    showErrorNotification,
    showMessageNotification,
  };
};

// 심각도에 따른 알림 타입 결정
function getNotificationTypeFromSeverity(severity: string): ChatNotification['type'] {
  switch (severity) {
    case 'critical':
    case 'high':
      return 'error';
    case 'medium':
      return 'warning';
    case 'low':
    default:
      return 'info';
  }
}