import { beforeEach, describe, expect, it, vi } from 'vitest';
import Navbar from './Navbar';
import Footer from './Footer';
import Layout from './Layout';
import NotificationsBell from './NotificationsBell';
import { click, flushPromises, render } from '../../test/render';

const authState = vi.hoisted(() => ({
  value: {
    user: null as null | { id: number; username: string; email: string; role: 'CLIENT' | 'FREELANCER' | 'ADMIN' },
    isAuthenticated: false,
    logout: vi.fn(),
  },
}));

const cartState = vi.hoisted(() => ({
  value: {
    items: [] as Array<{ gigId: number }>,
  },
}));

const api = vi.hoisted(() => ({
  get: vi.fn(),
  patch: vi.fn(),
}));

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => authState.value,
}));

vi.mock('../../context/CartContext', () => ({
  useCart: () => cartState.value,
}));

vi.mock('../../lib/api', () => ({ default: api }));

beforeEach(() => {
  vi.clearAllMocks();
  authState.value = { user: null, isAuthenticated: false, logout: vi.fn() };
  cartState.value = { items: [] };
  api.get.mockImplementation((url: string) =>
    Promise.resolve(url.includes('unread-count')
      ? { data: { data: { count: 3 } } }
      : {
          data: {
            data: [
              {
                id: 1,
                userId: 1,
                type: 'ORDER_UPDATE',
                title: 'Status promijenjen',
                content: 'Narudzba je isporucena.',
                referenceId: 11,
                isRead: false,
                createdAt: '2026-06-01T00:00:00Z',
              },
              {
                id: 2,
                userId: 1,
                type: 'SYSTEM',
                title: 'Sistem',
                content: 'Dobrodosli.',
                referenceId: null,
                isRead: true,
                createdAt: '2026-06-01T00:00:00Z',
              },
            ],
            meta: { total: 2 },
          },
        }),
  );
  api.patch.mockResolvedValue({ data: {} });
});

describe('layout components', () => {
  it('renders footer and layout shell', () => {
    const footer = render(<Footer />);
    expect(footer.text()).toContain('SkillBridge');
    expect(footer.text()).toContain('Kategorije');

    const layout = render(<Layout />);
    expect(layout.text()).toContain('Prijava');
    expect(layout.text()).toContain('SkillBridge');
  });

  it('renders anonymous, client and freelancer navbar branches', async () => {
    const anonymous = render(<Navbar />);
    expect(anonymous.text()).toContain('Prijava');
    expect(anonymous.text()).toContain('Registracija');

    authState.value = {
      user: { id: 1, username: 'client', email: 'client@example.com', role: 'CLIENT' },
      isAuthenticated: true,
      logout: vi.fn(),
    };
    cartState.value = { items: [{ gigId: 7 }] };
    const client = render(<Navbar />);
    await flushPromises();
    expect(client.text()).toContain('Dashboard');
    expect(client.container.querySelector('[aria-label="Korpa"]')).not.toBeNull();
    click(client.container.querySelector('button.flex'));
    expect(client.text()).toContain('Moj profil');
    click([...client.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Odjavi')) ?? null);
    expect(authState.value.logout).toHaveBeenCalledTimes(1);

    authState.value = {
      user: { id: 2, username: 'mila', email: 'mila@example.com', role: 'FREELANCER' },
      isAuthenticated: true,
      logout: vi.fn(),
    };
    const freelancer = render(<Navbar />);
    await flushPromises();
    expect(freelancer.text()).toContain('Kreiraj oglas');
  });

  it('loads, opens and marks notifications', async () => {
    authState.value = {
      user: { id: 1, username: 'client', email: 'client@example.com', role: 'CLIENT' },
      isAuthenticated: true,
      logout: vi.fn(),
    };
    const view = render(<NotificationsBell />);
    await flushPromises();

    expect(view.text()).toContain('3');
    click(view.container.querySelector('[aria-label="Obavještenja"], [aria-label="ObavjeÅ¡tenja"]'));
    expect(view.text()).toContain('Status promijenjen');
    click([...view.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Ozna')) ?? null);
    await flushPromises();
    expect(api.patch).toHaveBeenCalledWith('/notifications/read-all');

    click([...view.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Status promijenjen')) ?? null);
    await flushPromises();
    expect(api.patch).toHaveBeenCalledWith('/notifications/1/read');
  });
});
