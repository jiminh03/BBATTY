import { Post } from '../../entities/post/model/types'; // 경로는 상황에 따라 조정
import { PostStatus } from '../../entities/post/model/types'; 

export const dummyPosts: Post[] = [
  {
    id: '1',
    title: '제목인데… 아이디어 고갈',
    content: '이건 더미 게시글 내용입니다.',
    authorId: 'user-1',
    authorNickname: '빵지민',
    likes: 88,
    views: 3333,
    commentCount: 232,
    status: PostStatus.PUBLISHED, // PostStatus가 enum이면 정확히 맞춰야 함
    createdAt: new Date().toISOString(),
    updateAt: new Date().toISOString(),
  },
  {
    id: '2',
    title: '두 번째 글입니다',
    content: '두 번째 더미 게시글 내용이에요.',
    authorId: 'user-2',
    authorNickname: '홍길동',
    likes: 12,
    views: 104,
    commentCount: 8,
    status: PostStatus.PUBLISHED,
    createdAt: new Date().toISOString(),
    updateAt: new Date().toISOString(),
  },
];
