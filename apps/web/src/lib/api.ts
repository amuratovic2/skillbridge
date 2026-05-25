import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:3000/api';
let refreshRequest: Promise<string> | null = null;

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
});

function clearSession() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

export function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as
      | { message?: unknown; error?: unknown; errors?: unknown }
      | undefined;
    if (typeof data?.message === 'string' && data.message.trim()) {
      return data.message;
    }

    if (typeof data?.error === 'string' && data.error.trim()) {
      return data.error;
    }

    if (Array.isArray(data?.errors)) {
      const messages = data.errors.filter(
        (item): item is string => typeof item === 'string' && item.trim().length > 0,
      );
      if (messages.length > 0) {
        return messages.join(' ');
      }
    }

    if (data?.errors && typeof data.errors === 'object') {
      const messages = Object.values(data.errors).filter(
        (item): item is string => typeof item === 'string' && item.trim().length > 0,
      );
      if (messages.length > 0) {
        return messages.join(' ');
      }
    }
  }

  return fallback;
}

function refreshAccessToken() {
  if (!refreshRequest) {
    const refreshToken = localStorage.getItem('refreshToken');

    if (!refreshToken) {
      return Promise.reject(new Error('Missing refresh token'));
    }

    refreshRequest = axios
      .post(`${API_URL}/auth/refresh`, { refreshToken })
      .then((response) => {
        const { accessToken, refreshToken: newRefreshToken } = response.data?.data ?? {};

        if (typeof accessToken !== 'string' || !accessToken.trim()) {
          throw new Error('Invalid access token response');
        }

        if (typeof newRefreshToken !== 'string' || !newRefreshToken.trim()) {
          throw new Error('Invalid refresh token response');
        }

        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', newRefreshToken);

        return accessToken;
      })
      .finally(() => {
        refreshRequest = null;
      });
  }

  return refreshRequest;
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const requestUrl = originalRequest?.url || '';

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !requestUrl.includes('/auth/login') &&
      !requestUrl.includes('/auth/register') &&
      !requestUrl.includes('/auth/refresh')
    ) {
      originalRequest._retry = true;
      const currentAccessToken = localStorage.getItem('accessToken');
      const sentAccessToken = originalRequest.headers?.Authorization;

      if (currentAccessToken && sentAccessToken !== `Bearer ${currentAccessToken}`) {
        originalRequest.headers = originalRequest.headers || {};
        originalRequest.headers.Authorization = `Bearer ${currentAccessToken}`;
        return api(originalRequest);
      }

      try {
        const accessToken = await refreshAccessToken();
        originalRequest.headers = originalRequest.headers || {};
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch {
        clearSession();
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }

    return Promise.reject(error);
  },
);

export default api;
