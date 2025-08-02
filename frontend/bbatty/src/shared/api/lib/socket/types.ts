export interface SocketConfig {
  url: string;
  options?: {
    auth?: Record<string, any>;
    query?: Record<string, any>;
    [key: string]: any;
  };
}
