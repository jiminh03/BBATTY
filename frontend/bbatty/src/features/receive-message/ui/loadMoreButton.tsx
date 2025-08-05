import React from 'react';
import { View, StyleSheet, ActivityIndicator } from 'react-native';
import { Button } from '../../../shared/ui/atoms/button/button';
import { Label } from '../../../shared/ui/atoms/Label/Label';

interface LoadMoreButtonProps {
  onPress: () => void;
  isLoading: boolean;
  disabled?: boolean;
}

export const LoadMoreButton: React.FC<LoadMoreButtonProps> = ({
  onPress,
  isLoading,
  disabled = false
}) => {
  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="small" color="#007AFF" />
        <Label style={styles.loadingText}>이전 메시지를 불러오는 중...</Label>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Button
        onPress={onPress}
        disabled={disabled}
        style={[
        styles.button,
        ...(disabled ? [styles.disabledButton] : [])
        ]}
      >
        <Label style={[
          styles.buttonText,
          disabled && styles.disabledButtonText
        ]}>
          이전 메시지 더보기
        </Label>
      </Button>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingVertical: 16,
    alignItems: 'center',
  },
  button: {
    backgroundColor: '#F8F9FA',
    borderWidth: 1,
    borderColor: '#E9ECEF',
    borderRadius: 20,
    paddingHorizontal: 24,
    paddingVertical: 12,
    minHeight: 44,
    justifyContent: 'center',
    alignItems: 'center',
  },
  disabledButton: {
    backgroundColor: '#F1F3F4',
    borderColor: '#E8EAED',
  },
  buttonText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#007AFF',
  },
  disabledButtonText: {
    color: '#999',
  },
  loadingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 16,
  },
  loadingText: {
    fontSize: 14,
    color: '#666',
    marginLeft: 8,
  },
});