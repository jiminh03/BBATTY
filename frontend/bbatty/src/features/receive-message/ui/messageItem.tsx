import React from 'react';
import { View, StyleSheet, Text } from 'react-native';
import { Label } from '../../../shared/ui/atoms/Label/Label';
import { Image } from '../../../shared/ui/atoms/image/image';
import { ChatMessage, MatchChatMessage, SystemMessage } from '../model/types';
import { messageUtils } from '../utils/messageUtils';

interface MessageItemProps {
  message: ChatMessage;
  isMyMessage: boolean;
  showTimestamp?: boolean;
}

export const MessageItem: React.FC<MessageItemProps> = ({
  message,
  isMyMessage,
  showTimestamp = false
}) => {
  // 시스템 메시지 렌더링
  if (message.type !== 'message') {
    return <SystemMessageItem message={message as SystemMessage} />;
  }

  // 게임 채팅 (익명) vs 매칭 채팅 구분
  const isMatchChat = 'userId' in message;

  return (
    <View style={styles.messageContainer}>
      {showTimestamp && (
        <View style={styles.timestampContainer}>
          <Label style={styles.timestamp}>
            {messageUtils.formatMessageTime(message.timestamp)}
          </Label>
        </View>
      )}
      
      <View style={[
        styles.messageWrapper,
        isMyMessage ? styles.myMessageWrapper : styles.otherMessageWrapper
      ]}>
        {/* 다른 사람 메시지일 때만 프로필 표시 */}
        {!isMyMessage && isMatchChat && (
          <View style={styles.profileContainer}>
            <ProfileImage message={message as MatchChatMessage} />
          </View>
        )}
        
        <View style={[
          styles.messageBubble,
          isMyMessage ? styles.myMessageBubble : styles.otherMessageBubble
        ]}>
          {/* 매칭 채팅에서 다른 사람 메시지일 때 닉네임 표시 */}
          {!isMyMessage && isMatchChat && (
            <View style={styles.senderInfo}>
              <Label style={styles.senderName}>
                {(message as MatchChatMessage).nickname}
              </Label>
              {(message as MatchChatMessage).isVictoryFairy && (
                <Label style={styles.victoryFairy}>🧚‍♀️</Label>
              )}
            </View>
          )}
          
          <Label style={[
            styles.messageText,
            isMyMessage ? styles.myMessageText : styles.otherMessageText
          ]}>
            {message.content}
          </Label>
          
          <Label style={[
            styles.messageTime,
            isMyMessage ? styles.myMessageTime : styles.otherMessageTime
          ]}>
            {messageUtils.formatTime(message.timestamp)}
          </Label>
        </View>
      </View>
    </View>
  );
};

// 시스템 메시지 컴포넌트
const SystemMessageItem: React.FC<{ message: SystemMessage }> = ({ message }) => {
  const getSystemMessageText = (msg: SystemMessage): string => {
    switch (msg.type) {
      case 'user_join':
        return `${msg.userName || '사용자'}님이 입장했습니다.`;
      case 'user_leave':
        return `${msg.userName || '사용자'}님이 퇴장했습니다.`;
      case 'system':
        return msg.content;
      case 'error':
        return `오류: ${msg.content}`;
      default:
        return msg.content;
    }
  };

  return (
    <View style={styles.systemMessageContainer}>
      <Text style={styles.systemMessageText}>
        {getSystemMessageText(message)}
      </Text>
    </View>
  );
};

// 프로필 이미지 컴포넌트
const ProfileImage: React.FC<{ message: MatchChatMessage }> = ({ message }) => {
  return (
    <View style={styles.profileImageContainer}>
      {message.profileImgUrl ? (
        <Image
          source={{ uri: message.profileImgUrl }}
          style={styles.profileImage}
          defaultSource={require('../../../shared/assets/images/default-profile.png')}
        />
      ) : (
        <View style={styles.defaultProfileImage}>
          <Label style={styles.defaultProfileText}>
            {message.nickname.charAt(0).toUpperCase()}
          </Label>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  messageContainer: {
    marginVertical: 4,
  },
  timestampContainer: {
    alignItems: 'center',
    marginVertical: 12,
  },
  timestamp: {
    fontSize: 12,
    color: '#999',
    backgroundColor: '#F0F0F0',
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 12,
  },
  messageWrapper: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    marginBottom: 8,
  },
  myMessageWrapper: {
    justifyContent: 'flex-end',
  },
  otherMessageWrapper: {
    justifyContent: 'flex-start',
  },
  profileContainer: {
    marginRight: 8,
  },
  profileImageContainer: {
    width: 36,
    height: 36,
  },
  profileImage: {
    width: 36,
    height: 36,
    borderRadius: 18,
  },
  defaultProfileImage: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#E0E0E0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  defaultProfileText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#666',
  },
  messageBubble: {
    maxWidth: '75%',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 16,
  },
  myMessageBubble: {
    backgroundColor: '#007AFF',
    borderBottomRightRadius: 4,
  },
  otherMessageBubble: {
    backgroundColor: '#F0F0F0',
    borderBottomLeftRadius: 4,
  },
  senderInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  senderName: {
    fontSize: 12,
    fontWeight: '600',
    color: '#333',
  },
  victoryFairy: {
    fontSize: 12,
    marginLeft: 4,
  },
  messageText: {
    fontSize: 16,
    lineHeight: 22,
  },
  myMessageText: {
    color: '#FFFFFF',
  },
  otherMessageText: {
    color: '#333',
  },
  messageTime: {
    fontSize: 10,
    marginTop: 4,
  },
  myMessageTime: {
    color: 'rgba(255, 255, 255, 0.7)',
    textAlign: 'right',
  },
  otherMessageTime: {
    color: '#999',
  },
  systemMessageContainer: {
    alignItems: 'center',
    marginVertical: 8,
  },
  systemMessageText: {
    fontSize: 12,
    color: '#999',
    backgroundColor: 'rgba(0, 0, 0, 0.05)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 12,
  },
});