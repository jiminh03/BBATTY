export interface TimestampedEntity {
  createdAt: string;
  updateAt: string;
}

export interface BaseEntity extends TimestampedEntity {
  id: string;
}

export type RuntimeEnvironment = "development" | "production";

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

//export type SortOrder = 'asc' | 'desc';
