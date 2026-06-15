import { beforeEach, describe, expect, it, vi } from 'vitest';
import { click, flushPromises, render } from '../test/render';

const api = vi.hoisted(() => ({
  post: vi.fn(),
}));

vi.mock('../lib/api', () => ({ default: api }));

import { AuthProvider, useAuth, type User } from './AuthContext';

const user: User = {
  id: 1,
  username: 'client',
  email: 'client@example.com',
  role: 'CLIENT',
  firstName: 'Client',
};

function AuthProbe() {
  const auth = useAuth();

  return (
    <div>
      <p data-testid="state">
        {auth.isLoading ? 'loading' : 'ready'}:{auth.isAuthenticated ? auth.user?.username : 'guest'}
      </p>
      <button type="button" onClick={() => void auth.login('client@example.com', 'secret')}>
        Login
      </button>
      <button
        type="button"
        onClick={() => void auth.register({ username: 'newbie', email: 'new@example.com', password: 'secret', role: 'CLIENT' })}
      >
        Register
      </button>
      <button type="button" onClick={() => auth.setAuthenticatedUser({ ...user, username: 'manual' })}>
        Set manual
      </button>
      <button type="button" onClick={auth.logout}>Logout</button>
    </div>
  );
}

function authResponse(nextUser: User = user) {
  return {
    data: {
      data: {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        user: nextUser,
      },
    },
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('AuthProvider', () => {
  it('hydrates a valid stored session', async () => {
    localStorage.setItem('accessToken', 'stored-token');
    localStorage.setItem('user', JSON.stringify(user));

    const view = render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>,
      { router: false },
    );
    await flushPromises();

    expect(view.text()).toContain('ready:client');
  });

  it('clears an invalid stored session', async () => {
    localStorage.setItem('accessToken', 'stored-token');
    localStorage.setItem('refreshToken', 'stored-refresh');
    localStorage.setItem('user', JSON.stringify({ id: 'bad', role: 'CLIENT' }));

    const view = render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>,
      { router: false },
    );
    await flushPromises();

    expect(view.text()).toContain('ready:guest');
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('logs in, registers, sets a user manually and logs out', async () => {
    api.post
      .mockResolvedValueOnce(authResponse())
      .mockResolvedValueOnce(authResponse({ ...user, username: 'newbie' }))
      .mockResolvedValue({ data: {} });

    const view = render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>,
      { router: false },
    );
    await flushPromises();

    click(view.container.querySelector('button:nth-of-type(1)'));
    await flushPromises();
    expect(view.text()).toContain('ready:client');
    expect(localStorage.getItem('accessToken')).toBe('access-token');
    expect(api.post).toHaveBeenCalledWith('/auth/login', { email: 'client@example.com', password: 'secret' });

    click(view.container.querySelector('button:nth-of-type(2)'));
    await flushPromises();
    expect(view.text()).toContain('ready:newbie');
    expect(api.post).toHaveBeenCalledWith('/auth/register', {
      username: 'newbie',
      email: 'new@example.com',
      password: 'secret',
      role: 'CLIENT',
    });

    click(view.container.querySelector('button:nth-of-type(3)'));
    expect(view.text()).toContain('ready:manual');

    click(view.container.querySelector('button:nth-of-type(4)'));
    await flushPromises();
    expect(view.text()).toContain('ready:guest');
    expect(localStorage.getItem('user')).toBeNull();
    expect(api.post).toHaveBeenCalledWith('/auth/logout', { refreshToken: 'refresh-token' });
  });
});
