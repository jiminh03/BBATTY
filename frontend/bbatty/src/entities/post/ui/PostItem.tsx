import { Post } from '../model/types'
import { View, Text, StyleSheet } from 'react-native';


export const PostItem = ({ post }: { post: Post }) => {
  return (
    <View style={styles.container}>
  {/* 댓글 박스를 먼저 렌더링해서 absolute 배치 */}
  <View style={styles.commentBox}>
  <Text style={styles.commentTextNumber}>{post.commentCount}</Text>
  <Text style={styles.commentTextLabel}>댓글</Text>
</View>


  <Text style={styles.title}>{post.title}</Text>
  <Text style={styles.meta}>
    {post.authorNickname}   {new Date(post.createdAt).toLocaleString()}   👁 {post.views}   👍 {post.likes}
  </Text>
</View>

  );
};

const styles = StyleSheet.create({
  container: {
  padding: 12,
  backgroundColor: '#fff',
  borderBottomWidth: 1,
  borderColor: '#eee',
  position: 'relative', // 댓글 박스 기준
  minHeight: 80, // 내용에 따라 조절
  marginBottom: 3,
},

  title: {
    fontWeight: 'bold',
    fontSize: 16,
    marginBottom: 4,
  },
  meta: {
    fontSize: 12,
    color: '#888',
  },
  commentCount: {
    position: 'absolute',
    right: 16,
    top: 16,
    fontWeight: '600',
    color: '#333',
  },
  commentBox: {
  position: 'absolute',
  top: 8,
  right: 8,
  backgroundColor: '#f1f1f1',
  borderRadius: 12,
  width: 60,
  height: 60,
  justifyContent: 'center',
  alignItems: 'center',
},

commentText: {
  fontWeight: 'bold',
  color: '#333',
  fontSize: 13,
},

commentTextNumber: {
  fontWeight: 'bold',
  fontSize: 16,
  color: '#333',
},

commentTextLabel: {
  fontSize: 13,
  color: '#555',
},

});
