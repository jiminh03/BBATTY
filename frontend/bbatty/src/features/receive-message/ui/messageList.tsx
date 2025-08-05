import React, { useEffect, useRef, useCallback } from 'react';
import { FlatList, View, StyleSheet, RefreshControl, ActivityIndicator } from 'react-native';
import { MessageItem } from './messageItem';
import { LoadMoreButton } from './loadMoreButton';
import { useReceiveMessage } from '../api/useReceiveMessage';
import { ChatMessage } from '../model/types';
import { useConnectionStore } from '../../chat-connection';

interface MessageListProps {
  roomId: string;
  currentUserId?: string; // 현재 사용자 ID (내 메시지 구분용)
  onLoadMore?: () => void;
  style?: any;
}

export const MessageList: React.FC<MessageListProps> = ({
  roomId,
  currentUserId,
  onLoadMore,
  style
}) => {
  const flatListRef = useRef<FlatList>(null);
  const { messages, isLoading, hasMore, loadMoreMessages } = useReceiveMessage(roomId);
  const { isConnected } = useConnectionStore();

  // 새 메시지가 오면 자동 스크롤
  const scrollToBottom = useCallback(() => {
    if (messages.length > 0) {
      flatListRef.current?.scrollToEnd({ animated: true });
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
        disabled={!isConnected}
      />
    );
  }, [hasMore, handleLoadMore, isLoading, isConnected]);

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

  if (!isConnected) {
    return (
      <View style={[styles.container, styles.disconnectedContainer, style]}>
        {/* 연결 끊어짐 상태 표시 */}
      </View>
    );
  }

  return (
    <View style={[styles.container, style]}>
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
  disconnectedContainer: {
    justifyContent: 'center',
    alignItems: 'center',
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
});