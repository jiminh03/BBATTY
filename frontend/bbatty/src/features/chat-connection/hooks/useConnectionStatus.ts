import { useConnectionStore } from '../model/store';

export const useConnectionStatus = () => {
  const { 
    isConnecting, 
    isConnected, 
    connectionStatus, 
    error, 
    reconnectAttempts,
    lastConnectedAt 
  } = useConnectionStore();

  const isOnline = isConnected && connectionStatus === 'CONNECTED';
  const hasError = Boolean(error);
  const isReconnecting = reconnectAttempts > 0 && isConnecting;

  return {
    isConnecting,
    isConnected,
    isOnline,
    connectionStatus,
    error,
    hasError,
    reconnectAttempts,
    isReconnecting,
    lastConnectedAt,
    
    // 유틸리티 함수들
    canSendMessage: () => isOnline,
    getStatusText: () => {
      if (isConnecting) return '연결 중...';
      if (isOnline) return '온라인';
      if (hasError) return '연결 오류';
      if (isReconnecting) return `재연결 중... (${reconnectAttempts}회)`;
      return '오프라인';
    },
    getStatusColor: () => {
      if (isOnline) return 'green';
      if (isConnecting || isReconnecting) return 'yellow';
      if (hasError) return 'red';
      return 'gray';
    }
  };
};