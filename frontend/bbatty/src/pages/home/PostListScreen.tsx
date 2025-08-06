import { FlatList, View } from 'react-native';
import { PostItem } from '../../entities/post/ui/PostItem'; // 경로는 네 프로젝트에 맞게
import { Post } from '../../entities/post/model/types';
import { dummyPosts } from './dummyPosts';

export const PostListScreen = () => {
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