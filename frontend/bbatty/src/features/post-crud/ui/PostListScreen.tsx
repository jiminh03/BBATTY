import { PostItem } from '../../../entities/post/ui/PostListScreen'
import { Post, PostStatus } from '../../../entities/post/model/types'
import { View, FlatList } from 'react-native';
import { dummyPosts } from '../../../pages/home/dummyPosts';

const PostListScreen = () => {
  return (
    <View style={{ flex: 1 }}>
      <FlatList
        data={dummyPosts}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => <PostItem post={item} />}
      />
    </View>
  );
};


export default PostListScreen;

