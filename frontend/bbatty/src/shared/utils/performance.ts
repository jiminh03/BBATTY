/*
import { InteractionManager } from 'react-native';
import { useEffect, useRef, useCallback, useMemo, DependencyList } from 'react';

// 디바운스 훅
export const useDebounce = <T>(value: T, delay: number): T => {
  const [debouncedValue, setDebouncedValue] = React.useState<T>(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);

  return debouncedValue;
};

// 쓰로틀 훅
export const useThrottle = <T>(value: T, limit: number): T => {
  const [throttledValue, setThrottledValue] = React.useState<T>(value);
  const lastRan = useRef(Date.now());

  useEffect(() => {
    const handler = setTimeout(() => {
      if (Date.now() - lastRan.current >= limit) {
        setThrottledValue(value);
        lastRan.current = Date.now();
      }
    }, limit - (Date.now() - lastRan.current));

    return () => {
      clearTimeout(handler);
    };
  }, [value, limit]);

  return throttledValue;
};

// 디바운스 콜백
export const useDebouncedCallback = <T extends (...args: any[]) => any>(
  callback: T,
  delay: number,
  deps: DependencyList
): ((...args: Parameters<T>) => void) => {
  const timeoutRef = useRef(Timeout);

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  return useCallback(
    (...args: Parameters<T>) => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }

      timeoutRef.current = setTimeout(() => {
        callback(...args);
      }, delay);
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [...deps, delay]
  );
};

// 쓰로틀 콜백
export const useThrottledCallback = <T extends (...args: any[]) => any>(
  callback: T,
  limit: number,
  deps: DependencyList
): ((...args: Parameters<T>) => void) => {
  const lastRan = useRef(Date.now());

  return useCallback(
    (...args: Parameters<T>) => {
      if (Date.now() - lastRan.current >= limit) {
        callback(...args);
        lastRan.current = Date.now();
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [...deps, limit]
  );
};

// 인터랙션 완료 후 실행
export const runAfterInteractions = (callback: () => void): void => {
  InteractionManager.runAfterInteractions(() => {
    callback();
  });
};

// 인터랙션 훅
export const useAfterInteractions = (callback: () => void, deps: DependencyList): void => {
  useEffect(() => {
    const handle = InteractionManager.runAfterInteractions(() => {
      callback();
    });

    return () => handle.cancel();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
};

// 메모리 누수 방지를 위한 마운트 상태 추적
export const useIsMounted = (): (() => boolean) => {
  const isMountedRef = useRef(true);

  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  return useCallback(() => isMountedRef.current, []);
};

// 이전 값 추적
export const usePrevious = <T>(value: T): T | undefined => {
  const ref = useRef<T>();

  useEffect(() => {
    ref.current = value;
  }, [value]);

  return ref.current;
};

// 레이지 초기화
export const useLazyInitialization = <T>(factory: () => T): T => {
  const ref = useRef<{ initialized: boolean; value?: T }>({ initialized: false });

  if (!ref.current.initialized) {
    ref.current.value = factory();
    ref.current.initialized = true;
  }

  return ref.current.value as T;
};

// 배치 업데이트
export class BatchProcessor<T> {
  private batch: T[] = [];
  private timer: NodeJS.Timeout | null = null;
  private readonly batchSize: number;
  private readonly delay: number;
  private readonly processor: (items: T[]) => void;

  constructor(options: { batchSize: number; delay: number; processor: (items: T[]) => void }) {
    this.batchSize = options.batchSize;
    this.delay = options.delay;
    this.processor = options.processor;
  }

  add(item: T): void {
    this.batch.push(item);

    if (this.batch.length >= this.batchSize) {
      this.flush();
    } else if (!this.timer) {
      this.timer = setTimeout(() => this.flush(), this.delay);
    }
  }

  flush(): void {
    if (this.batch.length === 0) return;

    const items = [...this.batch];
    this.batch = [];

    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }

    this.processor(items);
  }

  clear(): void {
    this.batch = [];
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }
}

// 메모이제이션 헬퍼
export const memoize = <TArgs extends any[], TResult>(
  fn: (...args: TArgs) => TResult,
  keyResolver?: (...args: TArgs) => string
): ((...args: TArgs) => TResult) => {
  const cache = new Map<string, TResult>();

  return (...args: TArgs): TResult => {
    const key = keyResolver ? keyResolver(...args) : JSON.stringify(args);

    if (cache.has(key)) {
      return cache.get(key)!;
    }

    const result = fn(...args);
    cache.set(key, result);

    // 캐시 크기 제한 (LRU)
    if (cache.size > 100) {
      const firstKey = cache.keys().next().value;
      cache.delete(firstKey);
    }

    return result;
  };
};

// 리스트 가상화를 위한 아이템 높이 계산
export const calculateItemLayout = (itemHeight: number, itemCount: number) => ({
  getItemLayout: (_: any, index: number) => ({
    length: itemHeight,
    offset: itemHeight * index,
    index,
  }),
  getItemCount: () => itemCount,
  keyExtractor: (_: any, index: number) => String(index),
});

// 이미지 프리로드
export const preloadImages = async (urls: string[]): Promise<void> => {
  const promises = urls.map((url) => {
    return new Promise<void>((resolve, reject) => {
      Image.prefetch(url)
        .then(() => resolve())
        .catch(() => {
          // 실패해도 계속 진행
          console.warn(`Failed to preload image: ${url}`);
          resolve();
        });
    });
  });

  await Promise.all(promises);
};

// React Native의 Image import
import { Image } from 'react-native';
import React from 'react';
*/
