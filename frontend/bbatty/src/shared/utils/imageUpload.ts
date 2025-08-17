import { apiClient } from '../api';
import { wrapApiCall } from '../api/utils/apiWrapper';
import { Ok, Err, type Result } from './result';

interface ImageUploadError {
  type: 'PRESIGNED_URL_ERROR' | 'UPLOAD_ERROR' | 'FILE_ERROR';
  message: string;
}

interface ImageUploadResult {
  fileUrl: string;
  filePath: string;
}

interface PresignedUrlRequest {
  filename: string;
}

interface PresignedUrlResponse {
  uploadUrl: string;
  fileUrl: string;
  filePath: string;
}

const extractFileExtension = (fileName: string): string => {
  const extension = fileName.split('.').pop()?.toLowerCase();
  if (!extension || !['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(extension)) {
    throw new Error('지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp만 지원)');
  }
  return extension;
};

const generateUniqueFileName = (originalFileName: string, prefix: string = 'image'): string => {
  const extension = extractFileExtension(originalFileName);
  const timestamp = Date.now();
  const randomString = Math.random().toString(36).substring(2, 15);
  return `${prefix}_${timestamp}_${randomString}.${extension}`;
};

const getMimeType = (fileName: string): string => {
  const extension = fileName.split('.').pop()?.toLowerCase();
  switch (extension) {
    case 'jpg':
    case 'jpeg':
      return 'image/jpeg';
    case 'png':
      return 'image/png';
    case 'gif':
      return 'image/gif';
    case 'webp':
      return 'image/webp';
    default:
      return 'image/jpeg';
  }
};

const getPresignedUrl = (filename: string, endpoint: string = '/api/posts/images/presigned-url') =>
  wrapApiCall<PresignedUrlResponse>(() =>
    apiClient.post(endpoint, null, { params: { filename } } as any)
  );

export const uploadImageToS3 = async (
  fileUri: string,
  originalFileName: string,
  prefix: string = 'image',
  endpoint?: string
): Promise<Result<ImageUploadResult, ImageUploadError>> => {
  try {
    const uniqueFileName = generateUniqueFileName(originalFileName, prefix);

    const presignedResult = await getPresignedUrl(uniqueFileName, endpoint);
    if (!presignedResult.success) {
      return Err({
        type: 'PRESIGNED_URL_ERROR',
        message: 'presigned URL 요청 실패: ' + (presignedResult.error?.message || '알 수 없는 오류'),
      });
    }

    const { uploadUrl, fileUrl, filePath } = presignedResult.data;

    const response = await fetch(fileUri);
    const blob = await response.blob();

    const uploadResponse = await fetch(uploadUrl, {
      method: 'PUT',
      headers: {
        'Content-Type': getMimeType(uniqueFileName),
      },
      body: blob,
    });

    if (!uploadResponse.ok) {
      return Err({
        type: 'UPLOAD_ERROR',
        message: `S3 업로드 실패: ${uploadResponse.status} ${uploadResponse.statusText}`,
      });
    }

    return Ok({
      fileUrl,
      filePath,
    });
  } catch (error) {
    return Err({
      type: 'FILE_ERROR',
      message: error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.',
    });
  }
};