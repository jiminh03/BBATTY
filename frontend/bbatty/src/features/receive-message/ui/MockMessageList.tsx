import React, { useEffect, useRef, useCallback } from 'react';
import { FlatList, View, StyleSheet, RefreshControl, ActivityIndicator, Text } from 'react-native';
import { MessageItem } from './messageItem';
import { LoadMoreButton } from './loadMoreButton';
import { useMockReceiveMessage } from '../api/useMockReceiveMessage';
import { ChatMessage } from '../model/types';
import { MOCK_CURRENT_USER_ID } from '../utils/mockData';

interface MockMessageListProps {
  roomId: string;
  chatType?: 'match' | 'game';
  currentUserId?: string;
  enableRealTime?: boolean; // 실시간 메시지 시뮬레이션 여부
  onLoadMore?: () => void;
  style?: any;
}

export const MockMessageList: React.FC<MockMessageListProps> = ({
  roomId,
  chatType = 'match',
  currentUserId = MOCK_CURRENT_USER_ID,
  enableRealTime = true,
  onLoadMore,
  style
}) => {
  const flatListRef = useRef<FlatList>(null);
  const { 
    messages, 
    isLoading, 
    hasMore, 
    loadMoreMessages,
    startRealTimeSimulation,
    stopRealTimeSimulation,
    connectionError
  } = useMockReceiveMessage(roomId, chatType);

  // 실시간 시뮬레이션 제어
  useEffect(() => {
    if (enableRealTime) {
      startRealTimeSimulation();
    } else {
      stopRealTimeSimulation();
    }

    return () => {
      stopRealTimeSimulation();
    };
  }, [enableRealTime, startRealTimeSimulation, stopRealTimeSimulation]);

  // 새 메시지가 오면 자동 스크롤
  const scrollToBottom = useCallback(() => {
    if (messages.length > 0) {
      setTimeout(() => {
        flatListRef.current?.scrollToEnd({ animated: true });
      }, 100);
    }
  }, [messages.length]);

  useEffect(() => {
    scrollToBottom();
  }, [scrollToBottom]);

  const handleLoadMore = useCallback(async () => {
    if (hasMore && !isLoading) {
      await loadMoreMessages();
      onLoadMore?.();
    }
  }, [hasMore, isLoading, loadMoreMessages, onLoadMore]);

  const renderMessage = useCallback(({ item, index }: { item: ChatMessage; index: number }) => {
    const isMyMessage = currentUserId && 'userId' in item ? item.userId === currentUserId : false;
    const showTimestamp = index === 0 || 
      new Date(item.timestamp).getTime() - new Date(messages[index - 1]?.timestamp || 0).getTime() > 60000; // 1분 간격

    return (
      <MessageItem
        message={item}
        isMyMessage={isMyMessage}
        showTimestamp={showTimestamp}
      />
    );
  }, [currentUserId, messages]);

  const renderHeader = useCallback(() => {
    if (!hasMore) return null;
    
    return (
      <LoadMoreButton
        onPress={handleLoadMore}
        isLoading={isLoading}
        disabled={false} // 목업에서는 항상 활성화
      />
    );
  }, [hasMore, handleLoadMore, isLoading]);

  const renderFooter = useCallback(() => {
    if (!isLoading) return null;
    
    return (
      <View style={styles.loadingFooter}>
        <ActivityIndicator size="small" color="#007AFF" />
      </View>
    );
  }, [isLoading]);

  const keyExtractor = useCallback((item: ChatMessage, index: number) => {
    return item.messageId || `${item.timestamp}-${index}`;
  }, []);

  return (
    <View style={[styles.container, style]}>
      {/* 상단 정보 표시 */}
      <View style={styles.debugInfo}>
        <View style={styles.debugRow}>
          <View style={[styles.debugDot, { backgroundColor: '#4CAF50' }]} />
          <Text style={styles.debugText}>
            Mock Mode ({chatType}) - {messages.length}개 메시지
          </Text>
        </View>
        {connectionError && (
          <Text style={styles.errorText}>오류: {connectionError}</Text>
        )}
      </View>

      <FlatList
        ref={flatListRef}
        data={messages}
        renderItem={renderMessage}
        keyExtractor={keyExtractor}
        ListHeaderComponent={renderHeader}
        ListFooterComponent={renderFooter}
        showsVerticalScrollIndicator={false}
        maintainVisibleContentPosition={{
          minIndexForVisible: 1,
          autoscrollToTopThreshold: 10,
        }}
        onScrollToIndexFailed={() => {
          // 스크롤 실패 시 처리
        }}
        refreshControl={
          <RefreshControl
            refreshing={isLoading}
            onRefresh={handleLoadMore}
            enabled={hasMore}
          />
        }
        style={styles.flatList}
        contentContainerStyle={styles.contentContainer}
        inverted={false}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  flatList: {
    flex: 1,
  },
  contentContainer: {
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  loadingFooter: {
    paddingVertical: 16,
    alignItems: 'center',
  },
  debugInfo: {
    backgroundColor: '#F8F9FA',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#E9ECEF',
  },
  debugRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  debugDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 8,
  },
  debugText: {
    fontSize: 12,
    color: '#666',
  },
  errorText: {
    fontSize: 12,
    color: '#DC3545',
    marginTop: 4,
  },
});