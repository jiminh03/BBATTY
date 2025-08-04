import { API_CONFIG } from '../client/config';

export interface InfiniteScrollParams {
  limit: number;
  sortBy?: string;
}

export interface InfiniteScrollMeta {
  totalItems: number;
  hasNext: boolean;
  itemsPerPage: number;
}

export interface InfiniteScrollResponse<T> {
  items: T[];
  pagination: InfiniteScrollMeta;
}

export class InfiniteScrollBuilder {
  static createParams(limit = API_CONFIG.pagination.defaultLimit, sortBy = 'createdAt'): InfiniteScrollParams {
    return {
      limit: Math.min(limit, API_CONFIG.pagination.maxLimit),
      sortBy,
    };
  }

  static createResponse<T>(items: T[], totalItems: number, hasNext: boolean): InfiniteScrollResponse<T> {
    return {
      items,
      pagination: {
        totalItems,
        hasNext,
        itemsPerPage: items.length,
      },
    };
  }
}
