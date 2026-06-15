import { beforeEach, describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({
  delete: vi.fn(),
  get: vi.fn(),
  patch: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));

vi.mock('./api', () => ({ default: api }));

import { userDisplayName, userInitials, userServiceApi, type UserProfile } from './user-service';

const profile: UserProfile = {
  id: 2,
  username: 'mila',
  email: 'mila@example.com',
  role: 'FREELANCER',
  firstName: 'Mila',
  lastName: 'Kovac',
  country: 'BA',
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe('user display helpers', () => {
  it('builds display names and initials from strongest available fields', () => {
    expect(userDisplayName(profile)).toBe('Mila Kovac');
    expect(userDisplayName({ username: 'client7', email: 'c@example.com' })).toBe('client7');
    expect(userDisplayName({ username: '', email: 'fallback@example.com' })).toBe('fallback@example.com');

    expect(userInitials(profile)).toBe('MK');
    expect(userInitials({ username: 'client7', email: 'c@example.com' })).toBe('C');
    expect(userInitials({ username: '', email: '' })).toBe('?');
  });
});

describe('userServiceApi', () => {
  it('wraps profile read and update calls', async () => {
    api.get.mockResolvedValue({ data: { data: profile } });
    api.patch.mockResolvedValue({ data: { data: { ...profile, bio: 'New bio' } } });
    api.delete.mockResolvedValue({ data: { data: profile } });

    await expect(userServiceApi.me()).resolves.toEqual(profile);
    await expect(userServiceApi.byId(2)).resolves.toEqual(profile);
    await expect(userServiceApi.updateMe({ bio: 'New bio' })).resolves.toMatchObject({ bio: 'New bio' });
    await expect(userServiceApi.deactivateMe()).resolves.toEqual(profile);

    expect(api.get).toHaveBeenCalledWith('/users/me');
    expect(api.get).toHaveBeenCalledWith('/users/2');
    expect(api.patch).toHaveBeenCalledWith('/users/me', { bio: 'New bio' });
    expect(api.delete).toHaveBeenCalledWith('/users/me');
  });

  it('wraps list users and skill endpoints with fallback arrays', async () => {
    const skill = { id: 5, name: 'React' };
    api.get.mockImplementation((url: string) =>
      Promise.resolve(url === '/users'
        ? { data: { data: [profile], meta: { total: 1, page: 1, limit: 12, totalPages: 1 } } }
        : { data: { data: [skill] } }),
    );
    api.post.mockResolvedValue({ data: { data: skill } });
    api.put.mockResolvedValue({ data: { data: [skill] } });
    api.delete.mockResolvedValue({ data: { data: { message: 'removed' } } });

    await expect(userServiceApi.listUsers({ query: 'mila', limit: 12 })).resolves.toEqual({
      data: [profile],
      meta: { total: 1, page: 1, limit: 12, totalPages: 1 },
    });
    await expect(userServiceApi.allSkills()).resolves.toEqual([skill]);
    await expect(userServiceApi.createSkill('TypeScript')).resolves.toEqual(skill);
    await expect(userServiceApi.userSkills(2)).resolves.toEqual([skill]);
    await expect(userServiceApi.addMySkill(5)).resolves.toEqual(skill);
    await expect(userServiceApi.replaceMySkills([5])).resolves.toEqual([skill]);
    await expect(userServiceApi.removeMySkill(5)).resolves.toEqual({ message: 'removed' });

    expect(api.get).toHaveBeenCalledWith('/users', { params: { query: 'mila', limit: 12 } });
    expect(api.post).toHaveBeenCalledWith('/skills', { name: 'TypeScript' });
    expect(api.get).toHaveBeenCalledWith('/skills/user/2');
    expect(api.post).toHaveBeenCalledWith('/skills/me/5');
    expect(api.put).toHaveBeenCalledWith('/skills/me', { skillIds: [5] });
  });

  it('wraps portfolio endpoints', async () => {
    const item = { id: 4, userId: 2, title: 'Portfolio' };
    api.get.mockResolvedValue({ data: { data: [item] } });
    api.post.mockResolvedValue({ data: { data: item } });
    api.patch.mockResolvedValue({ data: { data: { ...item, title: 'Updated' } } });
    api.delete.mockResolvedValue({ data: { data: { message: 'deleted' } } });

    await expect(userServiceApi.userPortfolio(2)).resolves.toEqual([item]);
    await expect(userServiceApi.createPortfolioItem({ title: 'Portfolio' })).resolves.toEqual(item);
    await expect(userServiceApi.updatePortfolioItem(4, { title: 'Updated' })).resolves.toMatchObject({ title: 'Updated' });
    await expect(userServiceApi.deletePortfolioItem(4)).resolves.toEqual({ message: 'deleted' });

    expect(api.get).toHaveBeenCalledWith('/portfolios/user/2');
    expect(api.post).toHaveBeenCalledWith('/portfolios', { title: 'Portfolio' });
    expect(api.patch).toHaveBeenCalledWith('/portfolios/4', { title: 'Updated' });
    expect(api.delete).toHaveBeenCalledWith('/portfolios/4');
  });
});
