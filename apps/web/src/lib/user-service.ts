import api from './api';

export type UserRole = 'CLIENT' | 'FREELANCER' | 'ADMIN';

export interface Skill {
  id: number;
  name: string;
}

export interface PortfolioItem {
  id: number;
  userId: number;
  title: string;
  description?: string;
  imageUrl?: string;
  createdAt?: string;
}

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  firstName?: string;
  lastName?: string;
  bio?: string;
  profilePicture?: string;
  country?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
  skills?: Skill[];
  portfolioItems?: PortfolioItem[];
}

export interface PageMeta {
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface UpdateProfilePayload {
  firstName?: string;
  lastName?: string;
  bio?: string;
  profilePicture?: string;
  country?: string;
}

export interface PortfolioPayload {
  title?: string;
  description?: string;
  imageUrl?: string;
}

const unwrap = <T>(response: { data?: T }) => response.data as T;

export function userDisplayName(user: Pick<UserProfile, 'firstName' | 'lastName' | 'username' | 'email'>) {
  return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username || user.email;
}

export function userInitials(user: Pick<UserProfile, 'firstName' | 'lastName' | 'username' | 'email'>) {
  const parts = [user.firstName, user.lastName].filter((part): part is string => Boolean(part));
  const value = parts.length > 0
    ? parts.map((part) => part.charAt(0)).join('')
    : (user.username || user.email || '?').charAt(0);

  return value.toUpperCase();
}

export const userServiceApi = {
  me: () => api.get('/users/me').then((res) => unwrap<UserProfile>(res.data)),

  byId: (id: number | string) => api.get(`/users/${id}`).then((res) => unwrap<UserProfile>(res.data)),

  updateMe: (payload: UpdateProfilePayload) =>
    api.patch('/users/me', payload).then((res) => unwrap<UserProfile>(res.data)),

  listUsers: (params: {
    page?: number;
    limit?: number;
    query?: string;
    role?: UserRole;
    country?: string;
    skill?: string;
    sortBy?: string;
    sortDirection?: 'asc' | 'desc';
  }) =>
    api.get('/users', { params }).then((res) => ({
      data: unwrap<UserProfile[]>(res.data) ?? [],
      meta: (res.data.meta ?? { total: 0, page: 1, limit: params.limit ?? 12, totalPages: 1 }) as PageMeta,
    })),

  deactivateMe: () => api.delete('/users/me').then((res) => unwrap<UserProfile>(res.data)),

  allSkills: () => api.get('/skills').then((res) => unwrap<Skill[]>(res.data) ?? []),

  createSkill: (name: string) =>
    api.post('/skills', { name }).then((res) => unwrap<Skill>(res.data)),

  userSkills: (userId: number | string) =>
    api.get(`/skills/user/${userId}`).then((res) => unwrap<Skill[]>(res.data) ?? []),

  addMySkill: (skillId: number) =>
    api.post(`/skills/me/${skillId}`).then((res) => unwrap<{ message: string }>(res.data)),

  replaceMySkills: (skillIds: number[]) =>
    api.put('/skills/me', { skillIds }).then((res) => unwrap<Skill[]>(res.data) ?? []),

  removeMySkill: (skillId: number) =>
    api.delete(`/skills/me/${skillId}`).then((res) => unwrap<{ message: string }>(res.data)),

  userPortfolio: (userId: number | string) =>
    api.get(`/portfolios/user/${userId}`).then((res) => unwrap<PortfolioItem[]>(res.data) ?? []),

  createPortfolioItem: (payload: Required<Pick<PortfolioPayload, 'title'>> & PortfolioPayload) =>
    api.post('/portfolios', payload).then((res) => unwrap<PortfolioItem>(res.data)),

  updatePortfolioItem: (id: number, payload: PortfolioPayload) =>
    api.patch(`/portfolios/${id}`, payload).then((res) => unwrap<PortfolioItem>(res.data)),

  deletePortfolioItem: (id: number) =>
    api.delete(`/portfolios/${id}`).then((res) => unwrap<{ message: string }>(res.data)),
};
