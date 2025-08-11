export type QueryKey = (string | number | object)[];

export class QueryKeys {
  static all(): QueryKey {
    return ['api'];
  }

  // 엔티티별 키 생성
  static entity(entityName: string): QueryKey {
    return [...QueryKeys.all(), entityName];
  }

  // 검색 키
  static search(entityName: string, query: string, filters: Record<string, unknown> = {}): QueryKey {
    return [...QueryKeys.entity(entityName), 'search', query, filters];
  }

  // 상세 조회 키
  static detail(entityName: string, id: string | number): QueryKey {
    return [...QueryKeys.entity(entityName), 'detail', id];
  }

  // 무한 스크롤 키
  static infinite(entityName: string, filters: Record<string, unknown> = {}): QueryKey {
    return [...QueryKeys.entity(entityName), 'infinite', filters];
  }

  // 통계 키
  static stats(entityName: string, type = 'general', params: Record<string, unknown> = {}): QueryKey {
    return [...QueryKeys.entity(entityName), 'stats', type, params];
  }

  // 뱃지 키
  static badges(entityName: string, params: Record<string, unknown> = {}): QueryKey {
    return [...QueryKeys.entity(entityName), 'badges', params];
  }
}

interface QueryClient {
  invalidateQueries(options: { queryKey: QueryKey }): Promise<void>;
}

// 쿼리 무효화 유틸리티
export class QueryInvalidator {
  // 특정 엔티티의 모든 캐시 무효화
  static async invalidateEntity(queryClient: QueryClient, entityName: string): Promise<void> {
    await queryClient.invalidateQueries({
      queryKey: QueryKeys.entity(entityName),
    });
  }

  // 상세 조회 쿼리 캐시 무효화
  static async invalidateEntityItem(queryClient: QueryClient, entityName: string, id: string | number): Promise<void> {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: QueryKeys.detail(entityName, id),
      }),
    ]);
  }

  // 검색 결과 캐시 무효화
  static async invalidateSearchResults(queryClient: QueryClient, entityName: string): Promise<void> {
    await queryClient.invalidateQueries({
      queryKey: [...QueryKeys.entity(entityName), 'search'],
    });
  }

  // 무한 스크롤 캐시 무효화
  static async invalidateInfinite(queryClient: QueryClient, entityName: string): Promise<void> {
    await queryClient.invalidateQueries({
      queryKey: QueryKeys.infinite(entityName),
    });
  }

  // 통계 캐시 무효화
  static async invalidateStats(queryClient: QueryClient, entityName: string): Promise<void> {
    await queryClient.invalidateQueries({
      queryKey: [...QueryKeys.entity(entityName), 'stats'],
    });
  }

  // 뱃지 캐시 무효화
  static async invalidateBadges(queryClient: QueryClient, entityName: string): Promise<void> {
    await queryClient.invalidateQueries({
      queryKey: [...QueryKeys.entity(entityName), 'badges'],
    });
  }
}
