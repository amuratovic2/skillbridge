import { SERVICE_PORTS } from './constants';

export function getServiceUrl(envVar: string | undefined, defaultPort: number): string {
  return envVar || `http://localhost:${defaultPort}`;
}

export function getUserServiceUrl(): string {
  return getServiceUrl(process.env['USER_SERVICE_URL'], SERVICE_PORTS.USER_SERVICE);
}

export function getGigServiceUrl(): string {
  return getServiceUrl(process.env['GIG_SERVICE_URL'], SERVICE_PORTS.GIG_SERVICE);
}

export function getOrderServiceUrl(): string {
  return getServiceUrl(process.env['ORDER_SERVICE_URL'], SERVICE_PORTS.ORDER_SERVICE);
}

export function getCommServiceUrl(): string {
  return getServiceUrl(process.env['COMM_SERVICE_URL'], SERVICE_PORTS.COMM_SERVICE);
}
