export const SERVICE_PORTS = {
  API_GATEWAY: 3000,
  USER_SERVICE: 3001,
  GIG_SERVICE: 3002,
  ORDER_SERVICE: 3003,
  COMM_SERVICE: 3004,
} as const;

export const PAGINATION_DEFAULTS = {
  PAGE: 1,
  LIMIT: 12,
  MAX_LIMIT: 100,
} as const;

export const JWT_DEFAULTS = {
  ACCESS_EXPIRATION: '15m',
  REFRESH_EXPIRATION: '7d',
} as const;

export const ORDER_DEFAULTS = {
  MAX_REVISIONS: 3,
} as const;

export const GLOBAL_PREFIX = 'api';

export const INTERNAL_HEADERS = {
  USER_ID: 'x-user-id',
  USER_ROLE: 'x-user-role',
  USER_EMAIL: 'x-user-email',
} as const;
