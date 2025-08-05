interface PostFormData {
  title: string
  content: string
}

// 1. Boolean 반환: 유효성 체크만 필요한 경우
export const isValidPost = ({ title, content }: PostFormData): boolean => {
  return title.trim().length > 0 && content.trim().length > 0
}

// 2. 에러 메시지 반환: 사용자에게 피드백 보여줄 때
export const validatePostContent = (title: string, content: string): string | null => {
  if (title.trim().length === 0) return '제목을 입력해주세요.'
  if (content.trim().length === 0) return '내용을 입력해주세요.'
  return null
}
