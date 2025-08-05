// 댓글 단일 타입
export interface Comment {
  id: string
  postId: string
  authorId: string
  authorNickname: string
  authorProfileImage?: string
  content: string
  createdAt: string
  updatedAt: string
  isDeleted: boolean
  isMine?: boolean
  parentId?: string | null
  depth?: number
}
