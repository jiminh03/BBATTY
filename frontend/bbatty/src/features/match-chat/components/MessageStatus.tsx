import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import type { MessageStatus } from '../types';

interface MessageStatusProps {
  status?: MessageStatus;
  onRetry?: () => void;
  retryCount?: number;
  maxRetries?: number;
}

export const MessageStatusIndicator: React.FC<MessageStatusProps> = ({
  status = 'sent',
  onRetry,
  retryCount = 0,
  maxRetries = 3,
}) => {
  const getStatusConfig = () => {
    switch (status) {
      case 'sending':
        return {
          icon: <ActivityIndicator size="small" color="#F59E0B" />,
          text: '전송 중...',
          color: '#F59E0B',
          backgroundColor: 'rgba(245, 158, 11, 0.1)',
          showRetry: false,
        };
      case 'retrying':
        return {
          icon: <ActivityIndicator size="small" color="#F59E0B" />,
          text: `재시도 중... (${retryCount}/${maxRetries})`,
          color: '#F59E0B',
          backgroundColor: 'rgba(245, 158, 11, 0.1)',
          showRetry: false,
        };
      case 'failed':
        return {
          icon: <Text style={styles.failedIcon}>⚠️</Text>,
          text: '전송 실패',
          color: '#EF4444',
          backgroundColor: 'rgba(239, 68, 68, 0.1)',
          showRetry: true,
        };
      case 'sent':
      default:
        return {
          icon: <Text style={styles.sentIcon}>✓</Text>,
          text: '전송됨',
          color: '#10B981',
          backgroundColor: 'rgba(16, 185, 129, 0.1)',
          showRetry: false,
        };
    }
  };

  const statusConfig = getStatusConfig();

  if (status === 'sent') {
    // 전송 완료된 메시지는 상태 표시하지 않음 (또는 매우 간단하게)
    return null;
  }

  return (
    <View style={[styles.container, { backgroundColor: statusConfig.backgroundColor }]}>
      <View style={styles.statusContent}>
        {statusConfig.icon}
        <Text style={[styles.statusText, { color: statusConfig.color }]}>
          {statusConfig.text}
        </Text>
      </View>
      
      {statusConfig.showRetry && onRetry && retryCount < maxRetries && (
        <TouchableOpacity style={styles.retryButton} onPress={onRetry}>
          <Text style={styles.retryText}>재시도</Text>
        </TouchableOpacity>
      )}
      
      {status === 'failed' && retryCount >= maxRetries && (
        <Text style={styles.giveUpText}>재시도 한도 초과</Text>
      )}
    </View>
  );
};

// 메시지 옆에 붙는 간단한 상태 아이콘
interface SimpleMessageStatusProps {
  status?: MessageStatus;
  size?: number;
}

export const SimpleMessageStatus: React.FC<SimpleMessageStatusProps> = ({
  status = 'sent',
  size = 12,
}) => {
  const getStatusIcon = () => {
    switch (status) {
      case 'sending':
        return { icon: '⏳', color: '#F59E0B' };
      case 'retrying':
        return { icon: '🔄', color: '#F59E0B' };
      case 'failed':
        return { icon: '❌', color: '#EF4444' };
      case 'sent':
      default:
        return { icon: '✅', color: '#10B981' };
    }
  };

  const { icon, color } = getStatusIcon();

  return (
    <Text style={[styles.simpleIcon, { fontSize: size, color }]}>
      {icon}
    </Text>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    marginTop: 4,
  },
  statusContent: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  statusText: {
    fontSize: 12,
    fontWeight: '500',
    marginLeft: 6,
  },
  retryButton: {
    backgroundColor: '#EF4444',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    marginLeft: 8,
  },
  retryText: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '600',
  },
  giveUpText: {
    fontSize: 10,
    color: '#6B7280',
    marginLeft: 8,
  },
  failedIcon: {
    fontSize: 12,
  },
  sentIcon: {
    fontSize: 12,
    color: '#10B981',
  },
  simpleIcon: {
    marginLeft: 4,
  },
});