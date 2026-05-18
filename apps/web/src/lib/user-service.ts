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

export const userServiceApi = {
  me: () => api.get('/users/me').then((res) => unwrap<UserProfile>(res.data)),

  updateMe: (payload: UpdateProfilePayload) =>
    api.patch('/users/me', payload).then((res) => unwrap<UserProfile>(res.data)),

  listUsers: (params: {
    page?: number;
    limit?: number;
    query?: string;
    role?: UserRole;
    country?: string;
    skill?: string;
  }) =>
    api.get('/users', { params }).then((res) => ({
      data: unwrap<UserProfile[]>(res.data) ?? [],
      meta: (res.data.meta ?? { total: 0, page: 1, limit: params.limit ?? 12, totalPages: 1 }) as PageMeta,
    })),

  allSkills: () => api.get('/skills').then((res) => unwrap<Skill[]>(res.data) ?? []),

  addMySkill: (skillId: number) =>
    api.post(`/skills/me/${skillId}`).then((res) => unwrap<{ message: string }>(res.data)),

  removeMySkill: (skillId: number) =>
    api.delete(`/skills/me/${skillId}`).then((res) => unwrap<{ message: string }>(res.data)),

  createPortfolioItem: (payload: Required<Pick<PortfolioPayload, 'title'>> & PortfolioPayload) =>
    api.post('/portfolios', payload).then((res) => unwrap<PortfolioItem>(res.data)),

  updatePortfolioItem: (id: number, payload: PortfolioPayload) =>
    api.patch(`/portfolios/${id}`, payload).then((res) => unwrap<PortfolioItem>(res.data)),

  deletePortfolioItem: (id: number) =>
    api.delete(`/portfolios/${id}`).then((res) => unwrap<{ message: string }>(res.data)),
};
