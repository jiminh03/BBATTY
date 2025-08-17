// shared/utils/result.ts

//Result 타입 정의 - 성공(Ok) 또는 실패(Err) 중 하나의 상태를 가짐
export type Result<T, E = Error> = { success: true; data: T; error: null } | { success: false; data: null; error: E };

// 성공 결과 생성
export const Ok = <T>(data: T): Result<T, never> => ({
  success: true,
  data,
  error: null,
});

//실패 결과 생성
export const Err = <E>(error: E): Result<never, E> => ({
  success: false,
  data: null,
  error,
});

// 동기 함수를 Result 패턴으로 감싸기
export const wrapSync = <T, E = Error>(func: () => T, errorHandler?: (error: unknown) => E): Result<T, E> => {
  try {
    const result = func();
    return Ok(result);
  } catch (error) {
    console.error('Sync function error:', error);
    const processedError = errorHandler ? errorHandler(error) : (error as E);
    return Err(processedError);
  }
};

// 비동기 함수를 Result 패턴으로 감싸기
export const wrapAsync = async <T, E = Error>(
  func: () => Promise<T>,
  errorHandler?: (error: unknown) => E
): Promise<Result<T, E>> => {
  try {
    const result = await func();
    return Ok(result);
  } catch (error) {
    const processedError = errorHandler ? errorHandler(error) : (error as E);
    return Err(processedError);
  }
};

// Result 값이 성공인지 확인하는 타입 가드
export const isOk = <T, E>(result: Result<T, E>): result is { success: true; data: T; error: null } => {
  return result.success;
};

// Result 값이 실패인지 확인하는 타입 가드
export const isErr = <T, E>(result: Result<T, E>): result is { success: false; data: null; error: E } => {
  return !result.success;
};

// Result에서 데이터를 안전하게 추출 (성공한 경우만)
export const unwrap = <T, E>(result: Result<T, E>): T => {
  if (isOk(result)) {
    return result.data;
  }
  throw new Error('Called unwrap on an Err result');
};

// Result에서 데이터를 안전하게 추출 (실패 시 기본값 반환)
export const unwrapOr = <T, E>(result: Result<T, E>, defaultValue: T): T => {
  return isOk(result) ? result.data : defaultValue;
};

// Result에서 에러를 추출 (실패한 경우만)
export const unwrapErr = <T, E>(result: Result<T, E>): E => {
  if (isErr(result)) {
    return result.error;
  }
  throw new Error('Called unwrapErr on an Ok result');
};

// Result를 다른 타입으로 변환
export const mapResult = <T, U, E>(result: Result<T, E>, mapper: (data: T) => U): Result<U, E> => {
  return isOk(result) ? Ok(mapper(result.data)) : result;
};

// Result의 에러를 다른 타입으로 변환
export const mapErr = <T, E, F>(result: Result<T, E>, mapper: (error: E) => F): Result<T, F> => {
  return isErr(result) ? Err(mapper(result.error)) : result;
};

/**
 * 여러 Result를 하나로 합치기 (모두 성공해야 성공)
 */
export const combineResults = <T extends readonly unknown[], E>(results: { [K in keyof T]: Result<T[K], E> }): Result<
  T,
  E
> => {
  // 첫 번째 에러 찾기
  for (let i = 0; i < results.length; i++) {
    if (isErr(results[i])) {
      return results[i] as Result<never, E>;
    }
  }

  // 모든 결과가 성공이므로 데이터 추출
  const successData: unknown[] = [];
  for (let i = 0; i < results.length; i++) {
    const result = results[i];
    if (isOk(result)) {
      successData.push(result.data);
    }
  }

  return Ok(successData as unknown as T);
};

// 편의를 위한 타입 별칭들
export type AsyncResult<T, E = Error> = Promise<Result<T, E>>;
export type VoidResult<E = Error> = Result<void, E>;
export type AsyncVoidResult<E = Error> = Promise<Result<void, E>>;
