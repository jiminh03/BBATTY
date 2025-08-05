// comment/utils/validation.ts

export const isValidComment = (content: string): boolean => {
  const trimmed = content.trim()
  return trimmed.length > 0 && trimmed.length <= 200
}
