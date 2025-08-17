import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Animated,
  Dimensions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { ChatNotification } from '../types';

interface ChatNotificationProps {
  notification: ChatNotification;
  onDismiss: (id: string) => void;
}

const { width } = Dimensions.get('window');

export const ChatNotificationComponent: React.FC<ChatNotificationProps> = ({
  notification,
  onDismiss,
}) => {
  const [slideAnim] = useState(new Animated.Value(-100));
  const [opacityAnim] = useState(new Animated.Value(0));

  useEffect(() => {
    // 알림 표시 애니메이션
    Animated.parallel([
      Animated.timing(slideAnim, {
        toValue: 0,
        duration: 300,
        useNativeDriver: true,
      }),
      Animated.timing(opacityAnim, {
        toValue: 1,
        duration: 300,
        useNativeDriver: true,
      }),
    ]).start();

    // 자동 사라지기 (duration이 설정된 경우)
    if (notification.duration) {
      const timer = setTimeout(() => {
        dismissNotification();
      }, notification.duration);

      return () => clearTimeout(timer);
    }
  }, []);

  const dismissNotification = () => {
    Animated.parallel([
      Animated.timing(slideAnim, {
        toValue: -100,
        duration: 250,
        useNativeDriver: true,
      }),
      Animated.timing(opacityAnim, {
        toValue: 0,
        duration: 250,
        useNativeDriver: true,
      }),
    ]).start(() => {
      onDismiss(notification.id);
    });
  };

  const getNotificationStyle = () => {
    switch (notification.type) {
      case 'success':
        return { backgroundColor: '#10B981', borderColor: '#059669' };
      case 'warning':
        return { backgroundColor: '#F59E0B', borderColor: '#D97706' };
      case 'error':
        return { backgroundColor: '#EF4444', borderColor: '#DC2626' };
      default:
        return { backgroundColor: '#3B82F6', borderColor: '#2563EB' };
    }
  };

  return (
    <Animated.View
      style={[
        styles.container,
        getNotificationStyle(),
        {
          transform: [{ translateY: slideAnim }],
          opacity: opacityAnim,
        },
      ]}
    >
      <TouchableOpacity
        style={styles.content}
        onPress={dismissNotification}
        activeOpacity={0.9}
      >
        <Text style={styles.message}>{notification.message}</Text>
        
        {notification.action && (
          <TouchableOpacity
            style={styles.actionButton}
            onPress={notification.action.onPress}
          >
            <Text style={styles.actionText}>{notification.action.label}</Text>
          </TouchableOpacity>
        )}
      </TouchableOpacity>
    </Animated.View>
  );
};

// 알림 관리자 컴포넌트
interface ChatNotificationManagerProps {
  notifications: ChatNotification[];
  onDismiss: (id: string) => void;
}

export const ChatNotificationManager: React.FC<ChatNotificationManagerProps> = ({
  notifications,
  onDismiss,
}) => {
  const insets = useSafeAreaInsets();
  // 상단에 충분한 여백을 두고 표시
  const notificationTop = insets.top + 60; // safe area + 충분한 여백

  return (
    <View style={[styles.manager, { top: notificationTop }]}>
      {notifications.map((notification) => (
        <ChatNotificationComponent
          key={notification.id}
          notification={notification}
          onDismiss={onDismiss}
        />
      ))}
    </View>
  );
};

const styles = StyleSheet.create({
  manager: {
    position: 'absolute',
    left: 16,
    right: 16,
    zIndex: 9999,
  },
  container: {
    borderRadius: 12,
    marginBottom: 8,
    borderWidth: 1,
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
  },
  content: {
    padding: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  message: {
    flex: 1,
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '500',
    marginRight: 12,
  },
  actionButton: {
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
  },
  actionText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '600',
  },
});