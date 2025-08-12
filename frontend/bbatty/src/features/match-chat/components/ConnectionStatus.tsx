import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import type { ExtendedConnectionStatus } from '../types';

interface ConnectionStatusProps {
  status: ExtendedConnectionStatus;
  reconnectAttempts?: number;
  maxReconnectAttempts?: number;
}

export const ConnectionStatusIndicator: React.FC<ConnectionStatusProps> = ({
  status,
  reconnectAttempts = 0,
  maxReconnectAttempts = 3,
}) => {
  const getStatusConfig = () => {
    switch (status) {
      case 'CONNECTED':
        return {
          text: '연결됨',
          color: '#10B981',
          backgroundColor: '#D1FAE5',
          showIndicator: false,
        };
      case 'CONNECTING':
        return {
          text: '연결 중...',
          color: '#F59E0B',
          backgroundColor: '#FEF3C7',
          showIndicator: true,
        };
      case 'RECONNECTING':
        return {
          text: `재연결 중... (${reconnectAttempts}/${maxReconnectAttempts})`,
          color: '#F59E0B',
          backgroundColor: '#FEF3C7',
          showIndicator: true,
        };
      case 'OFFLINE':
        return {
          text: '오프라인',
          color: '#6B7280',
          backgroundColor: '#F3F4F6',
          showIndicator: false,
        };
      case 'ERROR':
        return {
          text: '연결 오류',
          color: '#EF4444',
          backgroundColor: '#FEE2E2',
          showIndicator: false,
        };
      case 'DISCONNECTED':
      default:
        return {
          text: '연결 안됨',
          color: '#6B7280',
          backgroundColor: '#F3F4F6',
          showIndicator: false,
        };
    }
  };

  const statusConfig = getStatusConfig();

  return (
    <View style={[styles.container, { backgroundColor: statusConfig.backgroundColor }]}>
      {statusConfig.showIndicator && (
        <ActivityIndicator 
          size="small" 
          color={statusConfig.color} 
          style={styles.indicator}
        />
      )}
      <View style={[styles.dot, { backgroundColor: statusConfig.color }]} />
      <Text style={[styles.text, { color: statusConfig.color }]}>
        {statusConfig.text}
      </Text>
    </View>
  );
};

// 헤더에 들어갈 간단한 상태 표시
interface SimpleConnectionStatusProps {
  status: ExtendedConnectionStatus;
}

export const SimpleConnectionStatus: React.FC<SimpleConnectionStatusProps> = ({
  status,
}) => {
  const getStatusConfig = () => {
    switch (status) {
      case 'CONNECTED':
        return { color: '#10B981', text: '●' };
      case 'CONNECTING':
      case 'RECONNECTING':
        return { color: '#F59E0B', text: '●' };
      case 'OFFLINE':
        return { color: '#6B7280', text: '●' };
      case 'ERROR':
        return { color: '#EF4444', text: '●' };
      case 'DISCONNECTED':
      default:
        return { color: '#6B7280', text: '○' };
    }
  };

  const statusConfig = getStatusConfig();

  return (
    <Text style={[styles.simpleStatus, { color: statusConfig.color }]}>
      {statusConfig.text}
    </Text>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    marginVertical: 4,
  },
  indicator: {
    marginRight: 6,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 6,
  },
  text: {
    fontSize: 12,
    fontWeight: '600',
  },
  simpleStatus: {
    fontSize: 16,
    fontWeight: 'bold',
  },
});