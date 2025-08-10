import React from 'react';
import { View, Text } from 'react-native';
import { styles } from './UserPostsPlaceholder.style';

interface UserPostsPlaceholderProps {
  userId: number;
  canViewPosts: boolean;
}

export const UserPostsPlaceholder: React.FC<UserPostsPlaceholderProps> = ({ userId, canViewPosts }) => {
  if (!canViewPosts) {
    return (
      <View style={styles.container}>
        <View style={styles.restrictedView}>
          <Text style={styles.restrictedText}>이 사용자는 게시글 조회를 허용하지 않습니다</Text>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>작성글</Text>
      <View style={styles.placeholder}>
        <Text style={styles.placeholderText}>
          {/* 여기에 다른 개발자가 작성한 PostList 컴포넌트를 import하여 사용 */}
          {`<PostList userId={${userId}} />`}
        </Text>
      </View>
    </View>
  );
};
