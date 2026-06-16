import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { change, click, flushPromises, input, render, submit } from '../test/render';
import LandingPage from './LandingPage';
import GigListingPage from './GigListingPage';
import GigDetailPage from './GigDetailPage';
import FreelancerProfilePage from './FreelancerProfilePage';
import UserDirectoryPage from './UserDirectoryPage';
import DashboardPage from './DashboardPage';
import MyOrdersPage from './MyOrdersPage';
import OrderStatsPage from './OrderStatsPage';
import OverdueOrdersPage from './OverdueOrdersPage';
import RevenuePage from './RevenuePage';
import LoginPage from './LoginPage';
import RegisterPage from './RegisterPage';
import CreateGigPage from './CreateGigPage';
import CartPage from './CartPage';
import NotFoundPage from './NotFoundPage';
import OrderCheckoutModal from './order/OrderCheckoutModal';
import type { Order } from '../lib/orders';

const api = vi.hoisted(() => ({
  delete: vi.fn(),
  get: vi.fn(),
  patch: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));

const authState = vi.hoisted(() => ({
  value: {
    user: null as null | { id: number; username: string; email: string; role: 'CLIENT' | 'FREELANCER' | 'ADMIN' },
    isAuthenticated: false,
    isLoading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    setAuthenticatedUser: vi.fn(),
  },
}));

const cartState = vi.hoisted(() => ({
  value: {
    items: [] as Array<{ gigId: number; title: string; cost: number; deliveryTime: number }>,
    add: vi.fn(),
    remove: vi.fn(),
    clear: vi.fn(),
    total: 0,
  },
}));

const toast = vi.hoisted(() => ({
  value: {
    error: vi.fn(),
    success: vi.fn(),
    info: vi.fn(),
    showToast: vi.fn(),
  },
}));

vi.mock('../lib/api', () => ({
  default: api,
  getApiErrorMessage: (_error: unknown, fallback: string) => fallback,
}));

vi.mock('../context/AuthContext', () => ({
  useAuth: () => authState.value,
  AuthProvider: ({ children }: { children: React.ReactNode }) => children,
}));

vi.mock('../context/CartContext', () => ({
  useCart: () => cartState.value,
  CartProvider: ({ children }: { children: React.ReactNode }) => children,
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => toast.value,
  ToastProvider: ({ children }: { children: React.ReactNode }) => children,
}));

const client = { id: 1, username: 'client', email: 'client@example.com', role: 'CLIENT' as const };
const freelancer = {
  id: 2,
  username: 'mila',
  email: 'mila@example.com',
  role: 'FREELANCER' as const,
  firstName: 'Mila',
  lastName: 'Kovac',
  country: 'BA',
  bio: 'Dizajn i frontend razvoj.',
  createdAt: '2026-01-01T00:00:00Z',
  skills: [{ id: 5, name: 'React' }],
  portfolioItems: [{ id: 9, userId: 2, title: 'Brand sistem', description: 'Vizuelni identitet' }],
};
const gig = {
  id: 7,
  title: 'Profesionalni logo paket',
  description: 'Detaljan opis logo paketa za male biznise.',
  freelancerId: 2,
  freelancerName: 'Mila Kovac',
  cost: 150,
  deliveryTime: 5,
  revisionCount: 2,
  coverImage: '',
  tags: [{ id: 1, name: 'logo' }],
};
const order: Order = {
  id: 11,
  clientId: 1,
  gigId: 7,
  sellerId: 2,
  totalCost: 150,
  status: 'COMPLETED',
  orderDate: '2026-06-01T00:00:00Z',
  deliveryDeadline: '2026-06-07T00:00:00Z',
  maxRevisions: 2,
  usedRevisions: 1,
  completedAt: '2026-06-08T00:00:00Z',
  cancelledAt: null,
};
const orderStatuses: Order['status'][] = [
  'PENDING',
  'ACCEPTED',
  'IN_PROGRESS',
  'DELIVERED',
  'REVISION_REQUESTED',
  'COMPLETED',
  'CANCELLED',
  'DISPUTED',
];
const allStatusOrders = orderStatuses.map((status, index) => ({
  ...order,
  id: 11 + index,
  status,
}));

function data<T>(value: T, meta?: unknown) {
  return Promise.resolve({ data: { data: value, meta } });
}

function setupApi() {
  api.get.mockImplementation((url: string, options?: unknown) => {
    if (url === '/categories') return data([{ id: 3, title: 'Dizajn' }]);
    if (url === '/gigs/featured?limit=6') return data([gig]);
    if (url.startsWith('/gigs/search')) return data([gig], { total: 1, page: 1, limit: 12, totalPages: 1 });
    if (url === '/gigs/freelancer/2') return data([gig]);
    if (url === '/gigs/7') return data(gig);
    if (url === '/users/2') return data(freelancer);
    if (url === '/users') return data([freelancer], { total: 1, page: 1, limit: 12, totalPages: 1 });
    if (url === '/users/me') return data({ ...client, firstName: 'Client' });
    if (url === '/skills') return data([{ id: 5, name: 'React' }]);
    if (url === '/skills/user/2') return data([{ id: 5, name: 'React' }]);
    if (url === '/portfolios/user/2') return data(freelancer.portfolioItems);
    if (url === '/reviews/rating/1' || url === '/reviews/rating/2') return data({ averageRating: 4.8, totalReviews: 12 });
    if (url === '/orders/my/buying') return data(allStatusOrders, { total: allStatusOrders.length, page: 1, limit: 10, totalPages: 1 });
    if (url === '/orders/my/selling') return data([order], { total: 1, page: 1, limit: 50, totalPages: 1 });
    if (url === '/orders/my/buying/status/PENDING') return data([{ ...order, status: 'PENDING' }]);
    if (url === '/orders/statistics/by-status') return data({ COMPLETED: 4, PENDING: 2 });
    if (url === '/orders/overdue') return data([{ ...order, status: 'IN_PROGRESS' }]);
    if (url === '/orders/my/revenue') return data(600);

    throw new Error(`Unhandled GET ${url} ${JSON.stringify(options)}`);
  });
  api.post.mockImplementation((url: string) => {
    if (url === '/orders/batch') return data([order]);
    if (url === '/gigs') return data(gig);
    return data({});
  });
  api.patch.mockResolvedValue({ data: { data: order } });
  api.delete.mockResolvedValue({ data: { data: {} } });
  api.put.mockResolvedValue({ data: { data: {} } });
}

function route(element: React.ReactElement, path: string, currentPath = path) {
  return render(
    <Routes>
      <Route path={path} element={element} />
      {path !== '*' && <Route path="*" element={<p>Fallback route</p>} />}
    </Routes>,
    { route: currentPath },
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  authState.value = {
    user: client,
    isAuthenticated: true,
    isLoading: false,
    login: vi.fn().mockResolvedValue(undefined),
    register: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn(),
    setAuthenticatedUser: vi.fn(),
  };
  cartState.value = {
    items: [{ gigId: 7, title: gig.title, cost: 150, deliveryTime: 5 }],
    add: vi.fn(),
    remove: vi.fn(),
    clear: vi.fn(),
    total: 150,
  };
  toast.value = {
    error: vi.fn(),
    success: vi.fn(),
    info: vi.fn(),
    showToast: vi.fn(),
  };
  setupApi();
});

describe('marketplace pages', () => {
  it('renders the landing page with featured gigs', async () => {
    authState.value = { ...authState.value, user: null, isAuthenticated: false };

    const view = route(<LandingPage />, '/');
    await flushPromises();

    expect(view.text()).toContain('SkillBridge');
    expect(view.text()).toContain('Profesionalni logo paket');
    expect(view.text()).toContain('Zapo');
  });

  it('renders gig search results with categories', async () => {
    const view = route(<GigListingPage />, '/gigs', '/gigs?q=logo&page=1');
    await flushPromises();

    expect(view.text()).toContain('Rezultati za "logo"');
    expect(view.text()).toContain('Dizajn');
    expect(view.text()).toContain('Profesionalni logo paket');
  });

  it('renders gig detail and lets authenticated clients add the gig to cart', async () => {
    cartState.value = { ...cartState.value, items: [], total: 0 };

    const view = route(<GigDetailPage />, '/gigs/:id', '/gigs/7');
    await flushPromises();

    expect(view.text()).toContain('Profesionalni logo paket');
    expect(view.text()).toContain('Mila Kovac');
    click([...view.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Dodaj u korpu')) ?? null);
    expect(cartState.value.add).toHaveBeenCalledWith({
      gigId: 7,
      title: gig.title,
      cost: 150,
      deliveryTime: 5,
    });
  });

  it('renders freelancer profile and directory data', async () => {
    const profile = route(<FreelancerProfilePage />, '/freelancer/:id', '/freelancer/2');
    await flushPromises();
    expect(profile.text()).toContain('Mila Kovac');
    expect(profile.text()).toContain('React');
    expect(profile.text()).toContain('Brand sistem');

    const directory = route(<UserDirectoryPage />, '/freelancers', '/freelancers?query=mila');
    await flushPromises();
    expect(directory.text()).toContain('Mila Kovac');
    expect(directory.text()).toContain('BA');
  });
});

describe('dashboard pages', () => {
  it('renders dashboard cards for client, freelancer and admin roles', async () => {
    const clientView = route(<DashboardPage />, '/dashboard');
    await flushPromises();
    expect(clientView.text()).toContain('Pretra');

    authState.value = { ...authState.value, user: { ...client, id: 2, role: 'FREELANCER', username: 'mila' } };
    const freelancerView = route(<DashboardPage />, '/dashboard');
    await flushPromises();
    expect(freelancerView.text()).toContain('Kreiraj uslugu');

    authState.value = { ...authState.value, user: { ...client, role: 'ADMIN', username: 'admin' } };
    const adminView = route(<DashboardPage />, '/dashboard');
    await flushPromises();
    expect(adminView.text()).toContain('Statistika');
  });

  it('renders orders, stats, overdue and revenue pages', async () => {
    const orders = route(<MyOrdersPage />, '/dashboard/orders');
    await flushPromises();
    expect(orders.text()).toContain('Profesionalni logo paket');
    expect(orders.text()).toContain('Mila Kovac');

    const stats = route(<OrderStatsPage />, '/dashboard/stats');
    await flushPromises();
    expect(stats.text()).toContain('Ukupno');
    expect(stats.text()).toContain('6');

    const overdue = route(<OverdueOrdersPage />, '/dashboard/overdue');
    await flushPromises();
    expect(overdue.text()).toContain('#11');

    const revenue = route(<RevenuePage />, '/dashboard/revenue');
    await flushPromises();
    expect(revenue.text()).toContain('600.00');
  });
});

describe('forms and cart pages', () => {
  it('validates login and register forms before submitting auth calls', async () => {
    const login = route(<LoginPage />, '/login');
    await submit(login.container.querySelector('form'));
    expect(login.text()).toContain('Unesite validan email');
    expect(authState.value.login).not.toHaveBeenCalled();

    const register = route(<RegisterPage />, '/register');
    await submit(register.container.querySelector('form'));
    expect(register.text()).toContain('Korisnicko ime mora imati');
    expect(authState.value.register).not.toHaveBeenCalled();
  });

  it('submits valid login and register forms', async () => {
    const login = route(<LoginPage />, '/login');
    input(login.container.querySelector('input[type="email"]'), 'client@example.com');
    input(login.container.querySelector('input[type="password"]'), 'secret123');
    await submit(login.container.querySelector('form'));
    expect(authState.value.login).toHaveBeenCalledWith('client@example.com', 'secret123');

    const register = route(<RegisterPage />, '/register');
    click([...register.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Freelancer')) ?? null);
    input(register.container.querySelector('input[name="firstName"]'), 'Mila');
    input(register.container.querySelector('input[name="lastName"]'), 'Kovac');
    input(register.container.querySelector('input[name="username"]'), 'mila_kovac');
    input(register.container.querySelector('input[name="email"]'), 'mila@example.com');
    input(register.container.querySelector('input[name="password"]'), 'secret123');
    input(register.container.querySelector('input[name="confirmPassword"]'), 'secret123');
    await submit(register.container.querySelector('form'));
    expect(authState.value.register).toHaveBeenCalledWith({
      username: 'mila_kovac',
      email: 'mila@example.com',
      password: 'secret123',
      role: 'FREELANCER',
      firstName: 'Mila',
      lastName: 'Kovac',
    });
  });

  it('submits a valid gig creation form', async () => {
    const view = route(<CreateGigPage />, '/dashboard/gigs/create');
    await flushPromises();

    input(view.container.querySelector('input[name="title"]'), 'Profesionalni logo dizajn');
    input(view.container.querySelector('textarea[name="description"]'), 'Detaljan opis usluge za klijente koji zele moderan logo.');
    input(view.container.querySelector('input[name="cost"]'), '150');
    input(view.container.querySelector('input[name="deliveryTime"]'), '5');
    input(view.container.querySelector('input[name="tags"]'), 'logo, brand');
    change(view.container.querySelector('select[name="categoryId"]'), '3');

    await submit(view.container.querySelector('form'));

    expect(api.post).toHaveBeenCalledWith('/gigs', {
      title: 'Profesionalni logo dizajn',
      description: 'Detaljan opis usluge za klijente koji zele moderan logo.',
      categoryId: 3,
      cost: 150,
      deliveryTime: 5,
      revisionCount: 3,
      tags: ['logo', 'brand'],
    });
  });

  it('checks out a full cart and keeps failed batch items in cart', async () => {
    api.post.mockResolvedValueOnce({ data: { data: [order] } });

    const view = route(<CartPage />, '/cart');

    expect(view.text()).toContain('Profesionalni logo paket');
    click([...view.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Naru')) ?? null);
    await flushPromises();

    expect(api.post).toHaveBeenCalledWith('/orders/batch', { orders: [{ gigId: 7 }] });
    expect(cartState.value.clear).toHaveBeenCalledTimes(1);

    cartState.value = {
      ...cartState.value,
      items: [
        { gigId: 7, title: gig.title, cost: 150, deliveryTime: 5 },
        { gigId: 8, title: 'SEO audit', cost: 50, deliveryTime: 2 },
      ],
      total: 200,
    };
    api.post.mockResolvedValueOnce({ data: { data: [order] } });

    const partial = route(<CartPage />, '/cart');
    click([...partial.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Naru')) ?? null);
    await flushPromises();

    expect(cartState.value.remove.mock.calls[0][0]).toBe(7);
    expect(partial.text()).toContain('Zapoceto je kreiranje 1 od 2');
  });

  it('creates an order through the checkout modal', async () => {
    api.post.mockResolvedValueOnce({ data: { data: order } });
    const onClose = vi.fn();
    const view = render(
      <OrderCheckoutModal
        gig={{ id: 7, title: gig.title, cost: 150, deliveryTime: 5, revisionCount: 2 }}
        onClose={onClose}
      />,
    );

    input(view.container.querySelector('textarea'), 'Trebaju mi SVG i PNG fajlovi.');
    await submit(view.container.querySelector('form'));
    await flushPromises();

    expect(api.post).toHaveBeenCalledWith('/orders', { gigId: 7, requirements: 'Trebaju mi SVG i PNG fajlovi.' });
    expect(view.text()).toContain('Narud');
    click([...view.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Moje')) ?? null);
  });

  it('renders not found page', () => {
    const view = route(<NotFoundPage />, '*', '/missing');
    expect(view.text()).toContain('404');
  });
});
