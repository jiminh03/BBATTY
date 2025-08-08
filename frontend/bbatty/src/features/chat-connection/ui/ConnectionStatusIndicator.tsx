import React, { useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { useConnectionStore } from '../model/store';
import { getErrorMessage } from '../../../shared/utils/error';
import { useToast } from '../../../app/providers/ToastProvider';

interface ConnectionStatusIndicatorProps {
  onRetry?: () => void;
  showRetryButton?: boolean;
  compact?: boolean;
}

export const ConnectionStatusIndicator: React.FC<ConnectionStatusIndicatorProps> = ({
  onRetry,
  showRetryButton = true,
  compact = false,
}) => {
  const { 
    connectionStatus, 
    error, 
    reconnectAttempts, 
    lastConnectedAt,
    client 
  } = useConnectionStore();
  
  const { showErrorToast, showToast } = useToast();

  // 연결 상태 변경 시 토스트 표시
  useEffect(() => {
    switch (connectionStatus) {
      case 'CONNECTED':
        showToast('채팅에 연결되었습니다.', 'success', 2000);
        break;
      case 'FAILED':
        if (error) {
          const chatError = getErrorMessage({ message: error });
          showErrorToast(chatError);
        }
        break;
      case 'RECONNECTING':
        showToast(`재연결 시도 중... (${reconnectAttempts}회)`, 'warning', 1500);
        break;
      default:
        break;
    }
  }, [connectionStatus, error, reconnectAttempts, showErrorToast, showToast]);

  const getStatusColor = () => {
    switch (connectionStatus) {
      case 'CONNECTED':
        return '#4CAF50';
      case 'CONNECTING':
      case 'RECONNECTING':
        return '#FF9800';
      case 'FAILED':
      case 'ERROR':
        return '#f44336';
      case 'DISCONNECTED':
      default:
        return '#757575';
    }
  };

  const getStatusText = () => {
    switch (connectionStatus) {
      case 'CONNECTED':
        return '연결됨';
      case 'CONNECTING':
        return '연결 중...';
      case 'RECONNECTING':
        return `재연결 중... (${reconnectAttempts}회)`;
      case 'FAILED':
        return '연결 실패';
      case 'ERROR':
        return '오류';
      case 'DISCONNECTED':
      default:
        return '연결 끊김';
    }
  };

  const getStatusIcon = () => {
    switch (connectionStatus) {
      case 'CONNECTED':
        return '🟢';
      case 'CONNECTING':
      case 'RECONNECTING':
        return '🟡';
      case 'FAILED':
      case 'ERROR':
        return '🔴';
      case 'DISCONNECTED':
      default:
        return '⚫';
    }
  };

  const handleRetry = () => {
    if (onRetry) {
      onRetry();
    } else if (client) {
      try {
        client.forceReconnect?.();
        showToast('재연결을 시도합니다...', 'info');
      } catch (error) {
        const chatError = getErrorMessage(error);
        showErrorToast(chatError);
      }
    }
  };

  if (compact) {
    return (
      <View style={styles.compactContainer}>
        <Text style={styles.statusIcon}>{getStatusIcon()}</Text>
        <Text style={[styles.compactText, { color: getStatusColor() }]}>
          {getStatusText()}
        </Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.statusRow}>
        <Text style={styles.statusIcon}>{getStatusIcon()}</Text>
        <Text style={[styles.statusText, { color: getStatusColor() }]}>
          {getStatusText()}
        </Text>
      </View>

      {error && (
        <Text style={styles.errorText} numberOfLines={2}>
          {error}
        </Text>
      )}

      {showRetryButton && (connectionStatus === 'FAILED' || connectionStatus === 'ERROR' || connectionStatus === 'DISCONNECTED') && (
        <TouchableOpacity style={styles.retryButton} onPress={handleRetry}>
          <Text style={styles.retryButtonText}>다시 연결</Text>
        </TouchableOpacity>
      )}

      {lastConnectedAt && connectionStatus !== 'CONNECTED' && (
        <Text style={styles.lastConnectedText}>
          마지막 연결: {new Date(lastConnectedAt).toLocaleTimeString()}
        </Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#f5f5f5',
    padding: 12,
    borderRadius: 8,
    marginBottom: 8,
  },
  compactContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  statusIcon: {
    fontSize: 12,
    marginRight: 6,
  },
  statusText: {
    fontSize: 14,
    fontWeight: '500',
  },
  compactText: {
    fontSize: 12,
    fontWeight: '500',
  },
  errorText: {
    fontSize: 12,
    color: '#666',
    marginBottom: 8,
    lineHeight: 16,
  },
  retryButton: {
    backgroundColor: '#2196F3',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 4,
    alignSelf: 'flex-start',
    marginBottom: 4,
  },
  retryButtonText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '500',
  },
  lastConnectedText: {
    fontSize: 10,
    color: '#999',
  },
});