import { createContext, useContext, useState, useEffect } from 'react';
import api from '../lib/api';
import type { ReactNode } from 'react';
import type { UserProfile, UserRole } from '../lib/user-service';

export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  firstName?: string;
  lastName?: string;
  bio?: string;
  profilePicture?: string;
  country?: string;
}

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: {
    username: string;
    email: string;
    password: string;
    role: 'CLIENT' | 'FREELANCER';
    firstName?: string;
    lastName?: string;
  }) => Promise<void>;
  logout: () => void;
  setAuthenticatedUser: (user: UserProfile) => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const VALID_ROLES = new Set<UserRole>(['CLIENT', 'FREELANCER', 'ADMIN']);

function isStoredUser(value: unknown): value is User {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<User>;
  return (
    typeof candidate.id === 'number' &&
    typeof candidate.username === 'string' &&
    candidate.username.trim().length > 0 &&
    typeof candidate.email === 'string' &&
    candidate.email.trim().length > 0 &&
    typeof candidate.role === 'string' &&
    VALID_ROLES.has(candidate.role as UserRole)
  );
}

function clearStoredSession() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0;
}

function readAuthPayload(responseData: unknown) {
  const payload = (responseData as { data?: unknown })?.data as
    | { accessToken?: unknown; refreshToken?: unknown; user?: unknown }
    | undefined;

  if (!isNonEmptyString(payload?.accessToken) || !isNonEmptyString(payload?.refreshToken) || !isStoredUser(payload?.user)) {
    throw new Error('Invalid auth response');
  }

  return {
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken,
    user: payload.user,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    const token = localStorage.getItem('accessToken');
    if (storedUser && token) {
      try {
        const parsed = JSON.parse(storedUser);
        if (isStoredUser(parsed)) {
          setUser(parsed);
        } else {
          clearStoredSession();
        }
      } catch {
        clearStoredSession();
      }
    }
    setIsLoading(false);
  }, []);

  const setAuthenticatedUser = (userData: UserProfile) => {
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
  };

  const login = async (email: string, password: string) => {
    const response = await api.post('/auth/login', { email, password });
    const { accessToken, refreshToken, user: userData } = readAuthPayload(response.data);

    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    setAuthenticatedUser(userData);
  };

  const register = async (data: {
    username: string;
    email: string;
    password: string;
    role: 'CLIENT' | 'FREELANCER';
    firstName?: string;
    lastName?: string;
  }) => {
    const response = await api.post('/auth/register', data);
    const { accessToken, refreshToken, user: userData } = readAuthPayload(response.data);

    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    setAuthenticatedUser(userData);
  };

  const logout = () => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      api.post('/auth/logout', { refreshToken }).catch(() => {});
    }
    clearStoredSession();
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        login,
        register,
        logout,
        setAuthenticatedUser,
        isAuthenticated: !!user,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
