export interface TimestampedEntity {
  createdAt: string;
  updateAt: string;
}

export interface BaseEntity extends TimestampedEntity {
  id: string;
}

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
