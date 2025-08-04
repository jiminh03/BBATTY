import { PostStatus } from '../model/types';

export const getPostStatusLabel = (status: PostStatus): string => {
  switch (status) {
    case PostStatus.DRAFT:
      return '임시 저장됨';
    case PostStatus.PUBLISHED:
      return '게시됨';
    case PostStatus.ARCHIVED:
      return '보관됨';
    case PostStatus.DELETED:
      return '삭제됨';
    default:
      return '알 수 없음';
  }
};
