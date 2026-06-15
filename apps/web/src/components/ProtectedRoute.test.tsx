import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import { render } from '../test/render';

const authState = vi.hoisted(() => ({
  value: {
    user: null as null | { id: number; role: string; username: string; email: string },
    isLoading: false,
    isAuthenticated: false,
  },
}));

vi.mock('../context/AuthContext', () => ({
  useAuth: () => authState.value,
}));

beforeEach(() => {
  authState.value = {
    user: null,
    isLoading: false,
    isAuthenticated: false,
  };
});

function ProtectedView({ roles }: { roles?: string[] }) {
  return (
    <Routes>
      <Route
        path="/private"
        element={
          <ProtectedRoute roles={roles}>
            <p>Secret content</p>
          </ProtectedRoute>
        }
      />
      <Route path="/login" element={<p>Login page</p>} />
      <Route path="/" element={<p>Home page</p>} />
    </Routes>
  );
}

describe('ProtectedRoute', () => {
  it('shows a loading spinner while auth is loading', () => {
    authState.value = { user: null, isLoading: true, isAuthenticated: false };

    const view = render(<ProtectedView />, { route: '/private' });

    expect(view.container.querySelector('.animate-spin')).not.toBeNull();
  });

  it('redirects anonymous users to login', () => {
    const view = render(<ProtectedView />, { route: '/private' });

    expect(view.text()).toContain('Login page');
  });

  it('redirects users without the required role home', () => {
    authState.value = {
      user: { id: 1, role: 'CLIENT', username: 'client', email: 'client@example.com' },
      isLoading: false,
      isAuthenticated: true,
    };

    const view = render(<ProtectedView roles={['FREELANCER']} />, { route: '/private' });

    expect(view.text()).toContain('Home page');
  });

  it('renders children when the user is authenticated and authorized', () => {
    authState.value = {
      user: { id: 2, role: 'FREELANCER', username: 'mila', email: 'mila@example.com' },
      isLoading: false,
      isAuthenticated: true,
    };

    const view = render(<ProtectedView roles={['FREELANCER']} />, { route: '/private' });

    expect(view.text()).toContain('Secret content');
  });
});
