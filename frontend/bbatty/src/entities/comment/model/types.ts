// entities/comment/model/types.ts
export interface Comment {
  id: number | string;
  content: string;
  createdAt: string;
  updatedAt: string;
  isDeleted: boolean;
  authorNickname: string;
  userId: number;
  
  // 서버가 줄 수도, 안 줄 수도 있는 필드들
  depth?: number;
  parentId?: number | null;
  isMine?: boolean;
  authorId?: number | string;
  writerId?: number | string;
}
