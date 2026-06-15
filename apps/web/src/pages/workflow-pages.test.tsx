import { beforeEach, describe, expect, it, vi } from 'vitest';
import { act } from 'react';
import { Route, Routes } from 'react-router-dom';
import { change, click, flushPromises, input, render, submit } from '../test/render';
import MessagesPage from './MessagesPage';
import CustomOffersPage from './CustomOffersPage';
import NewCustomOfferModal from './NewCustomOfferModal';
import OrderDetailPage from './OrderDetailPage';
import ProfileSettingsPage from './ProfileSettingsPage';
import EditGigPage from './EditGigPage';
import AdminOrderTools from './AdminOrderTools';
import CancelOrderModal from './order/CancelOrderModal';
import RevisionRequestModal from './order/RevisionRequestModal';
import DeliverWorkModal from './order/DeliverWorkModal';
import DeliveryDetailModal from './order/DeliveryDetailModal';
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
    user: { id: 1, username: 'client', email: 'client@example.com', role: 'CLIENT' as const },
    isAuthenticated: true,
    isLoading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    setAuthenticatedUser: vi.fn(),
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
}));

vi.mock('../context/ToastContext', () => ({
  useToast: () => toast.value,
}));

const client = {
  id: 1,
  username: 'client',
  email: 'client@example.com',
  role: 'CLIENT' as const,
  firstName: 'Client',
  lastName: 'User',
  country: 'BA',
};
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
  categoryId: 3,
  cost: 150,
  deliveryTime: 5,
  revisionCount: 2,
  tags: [{ id: 1, name: 'logo' }],
};
const order: Order = {
  id: 11,
  clientId: 1,
  gigId: 7,
  sellerId: 2,
  totalCost: 150,
  status: 'DELIVERED',
  orderDate: '2026-06-01T00:00:00Z',
  deliveryDeadline: '2026-06-07T00:00:00Z',
  maxRevisions: 2,
  usedRevisions: 1,
  completedAt: null,
  cancelledAt: null,
  requirements: 'Treba mi minimalisticki logo.',
  history: [
    {
      id: 1,
      changedByUserId: 2,
      actionType: 'STATUS',
      oldStatus: 'IN_PROGRESS',
      newStatus: 'DELIVERED',
      note: 'Isporuceno',
      changedAt: '2026-06-05T10:00:00Z',
    },
  ],
};
let currentOrder: Order = order;
const delivery = {
  id: 3,
  orderId: 11,
  versionNumber: 1,
  message: 'Prva verzija je spremna.',
  fileUrl: 'https://example.com/logo.zip',
  fileName: 'logo.zip',
  createdAt: '2026-06-05T10:00:00Z',
};
const message = {
  id: 4,
  senderId: 2,
  receiverId: 1,
  orderId: 11,
  content: 'Pogledajte isporuku.',
  isRead: false,
  sentAt: '2026-06-05T10:30:00Z',
};
const offer = {
  id: 6,
  gigId: 7,
  orderId: null,
  senderId: 2,
  receiverId: 1,
  title: 'Posebna ponuda za logo',
  description: 'Ukljucuje dodatne formate.',
  price: 180,
  deliveryDays: 6,
  revisionCount: 2,
  status: 'PENDING' as const,
  expiresAt: null,
  createdAt: '2026-06-04T00:00:00Z',
};

function data<T>(value: T, meta?: unknown) {
  return Promise.resolve({ data: { data: value, meta } });
}

function setupApi() {
  api.get.mockImplementation((url: string) => {
    if (url === '/messages/conversations') return data([{ partnerId: 2, lastMessage: 'Pogledajte isporuku.', lastAt: '2026-06-05T10:30:00Z', unreadCount: 1 }]);
    if (url === '/messages/conversation/2') return data([message], { total: 1, page: 1, limit: 50, totalPages: 1 });
    if (url === '/messages/order/11') return data([message]);
    if (url === '/users/1') return data(client);
    if (url === '/users/2') return data(freelancer);
    if (url === '/users/me') return data(freelancer);
    if (url === '/users') return data([client], { total: 1, page: 1, limit: 8, totalPages: 1 });
    if (url === '/skills') return data([{ id: 5, name: 'React' }, { id: 6, name: 'TypeScript' }]);
    if (url === '/gigs/7') return data(gig);
    if (url === '/categories') return data([{ id: 3, title: 'Dizajn' }]);
    if (url === '/gigs/freelancer/2') return data([gig]);
    if (url === '/deliveries/order/11') return data([delivery]);
    if (url === '/deliveries/order/11/version/1') return data(delivery);
    if (url === '/orders/11') return data(currentOrder);
    if (url === '/custom-offers/received') return data([offer]);
    if (url === '/custom-offers/sent') return data([{ ...offer, senderId: 2, receiverId: 1 }]);
    if (url === '/portfolios/user/2') return data(freelancer.portfolioItems);
    if (url === '/skills/user/2') return data(freelancer.skills);
    throw new Error(`Unhandled GET ${url}`);
  });
  api.post.mockImplementation((url: string) => {
    if (url === '/messages') return data({ ...message, content: 'Nova poruka' });
    if (url === '/deliveries/order/11') return data({ ...delivery, versionNumber: 2 });
    if (url === '/orders/11/revision') return data({ ...order, status: 'REVISION_REQUESTED' });
    if (url === '/custom-offers') return data(offer);
    if (url === '/skills') return data({ id: 8, name: 'UX' });
    if (url.startsWith('/skills/me/')) return data({ message: 'ok' });
    return data({});
  });
  api.patch.mockImplementation((url: string) => {
    if (url === '/users/me') return data({ ...freelancer, bio: 'Azuriran bio' });
    if (url === '/portfolios/9') return data({ ...freelancer.portfolioItems[0], title: 'Azuriran portfolio' });
    if (url === '/orders/11/status') return data({ ...order, status: 'COMPLETED' });
    if (url === '/orders/11') return data({ ...order, totalCost: 175 });
    if (url === '/custom-offers/6/respond') return data({ ...offer, status: 'REJECTED' });
    if (url === '/custom-offers/6/withdraw') return data({ ...offer, status: 'WITHDRAWN' });
    return data({});
  });
  api.put.mockResolvedValue({ data: { data: [] } });
  api.delete.mockImplementation((url: string) => {
    if (url === '/users/me') return data(client);
    if (url === '/portfolios/9') return data({ message: 'deleted' });
    if (url === '/skills/me/5') return data({ message: 'removed' });
    return data({});
  });
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
    user: { id: 1, username: 'client', email: 'client@example.com', role: 'CLIENT' },
    isAuthenticated: true,
    isLoading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    setAuthenticatedUser: vi.fn(),
  };
  toast.value = {
    error: vi.fn(),
    success: vi.fn(),
    info: vi.fn(),
    showToast: vi.fn(),
  };
  setupApi();
  currentOrder = order;
});

describe('messaging and offer workflows', () => {
  it('renders messages and sends a new message', async () => {
    const view = route(<MessagesPage />, '/dashboard/messages', '/dashboard/messages?to=2');
    await flushPromises();

    expect(view.text()).toContain('Poruke');
    expect(view.text()).toContain('Pogledajte isporuku.');

    input(view.container.querySelector('input[placeholder="Unesite poruku..."]'), 'Hvala, pregledam.');
    await submit(view.container.querySelector('form'));

    expect(api.post).toHaveBeenCalledWith('/messages', { receiverId: 2, content: 'Hvala, pregledam.' });
  });

  it('renders custom offers and handles received offer response', async () => {
    const view = route(<CustomOffersPage />, '/dashboard/custom-offers');
    await flushPromises();

    expect(view.text()).toContain('Posebna ponuda za logo');
    click([...view.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Odbij')) ?? null);
    await flushPromises();

    expect(api.patch).toHaveBeenCalledWith('/custom-offers/6/respond', { status: 'REJECTED' });
  });

  it('creates a new custom offer from the modal', async () => {
    vi.useFakeTimers();
    authState.value = {
      ...authState.value,
      user: { id: 2, username: 'mila', email: 'mila@example.com', role: 'FREELANCER' },
    };
    const onClose = vi.fn();
    const onCreated = vi.fn();
    const view = render(<NewCustomOfferModal onClose={onClose} onCreated={onCreated} />, { router: false });
    await flushPromises();

    await submit(view.container.querySelector('form'));
    expect(view.text()).toContain('Odaberite');

    input(view.container.querySelector('input[type="search"]'), 'client');
    await act(async () => {
      vi.advanceTimersByTime(260);
      await Promise.resolve();
    });
    await flushPromises();
    click([...view.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Client User')) ?? null);
    change(view.container.querySelector('select'), '7');
    const fields = view.container.querySelectorAll('input');
    input(fields[0] as HTMLInputElement, 'Logo prosireni paket');
    input(view.container.querySelector('textarea'), 'Dodatni formati i brzi rok.');
    input(fields[1] as HTMLInputElement, '180');

    await submit(view.container.querySelector('form'));
    expect(api.post).toHaveBeenCalledWith('/custom-offers', expect.objectContaining({ receiverId: 1, price: 180 }));
    expect(onCreated).toHaveBeenCalledTimes(1);
  });
});

describe('order detail workflows', () => {
  it('renders order detail, sends a message and opens delivery detail', async () => {
    const view = route(<OrderDetailPage />, '/dashboard/orders/:id', '/dashboard/orders/11');
    await flushPromises();

    expect(view.text()).toContain('Narud');
    expect(view.text()).toContain('Treba mi minimalisticki logo.');

    input(view.container.querySelector('input[placeholder="Unesite poruku..."]'), 'Hvala!');
    await submit(view.container.querySelector('form'));
    expect(api.post).toHaveBeenCalledWith('/messages', { receiverId: 2, orderId: 11, content: 'Hvala!' });

    click([...view.container.querySelectorAll('button')].find((button) => button.textContent?.includes('Verzija 1')) ?? null);
    await flushPromises();
    expect(view.text()).toContain('Prva verzija je spremna.');
  });

  it('covers order detail action copy for seller, client, admin and terminal statuses', async () => {
    const cases: Array<{ status: Order['status']; user: typeof authState.value.user; expected: string }> = [
      { status: 'PENDING', user: { id: 2, username: 'mila', email: 'mila@example.com', role: 'FREELANCER' }, expected: 'Prihvatite' },
      { status: 'ACCEPTED', user: { id: 2, username: 'mila', email: 'mila@example.com', role: 'FREELANCER' }, expected: 'Zapo' },
      { status: 'IN_PROGRESS', user: { id: 2, username: 'mila', email: 'mila@example.com', role: 'FREELANCER' }, expected: 'Po' },
      { status: 'REVISION_REQUESTED', user: { id: 2, username: 'mila', email: 'mila@example.com', role: 'FREELANCER' }, expected: 'Klijent' },
      { status: 'COMPLETED', user: { id: 1, username: 'client', email: 'client@example.com', role: 'CLIENT' }, expected: 'zavr' },
      { status: 'CANCELLED', user: { id: 1, username: 'client', email: 'client@example.com', role: 'CLIENT' }, expected: 'otkazana' },
      { status: 'DISPUTED', user: { id: 99, username: 'admin', email: 'admin@example.com', role: 'ADMIN' }, expected: 'Rije' },
    ];

    for (const item of cases) {
      currentOrder = { ...order, status: item.status };
      authState.value = { ...authState.value, user: item.user };
      const view = route(<OrderDetailPage />, '/dashboard/orders/:id', '/dashboard/orders/11');
      await flushPromises();
      expect(view.text()).toContain(item.expected);
      view.unmount();
    }
  });

  it('exercises order modals and admin tools', async () => {
    const onDone = vi.fn();
    const onClose = vi.fn();
    const onChange = vi.fn();

    const cancel = render(<CancelOrderModal orderId={11} onClose={onClose} onDone={onDone} />, { router: false });
    input(cancel.container.querySelector('textarea'), 'Klijent odustao od narudzbe.');
    await submit(cancel.container.querySelector('form'));
    expect(api.patch).toHaveBeenCalledWith('/orders/11/status', { status: 'CANCELLED', note: 'Klijent odustao od narudzbe.' });

    const revision = render(<RevisionRequestModal orderId={11} revisionsLeft={1} onClose={onClose} onDone={onDone} />, { router: false });
    input(revision.container.querySelector('textarea'), 'Molim manje izmjene.');
    await submit(revision.container.querySelector('form'));
    expect(api.post).toHaveBeenCalledWith('/orders/11/revision', { message: 'Molim manje izmjene.' });

    const deliver = render(<DeliverWorkModal orderId={11} nextVersion={2} onClose={onClose} onDone={onDone} />, { router: false });
    input(deliver.container.querySelector('textarea'), 'Nova verzija je spremna.');
    input(deliver.container.querySelector('input[type="url"]'), 'https://example.com/file.zip');
    input(deliver.container.querySelector('input[type="text"]'), 'file.zip');
    await submit(deliver.container.querySelector('form'));
    expect(api.post).toHaveBeenCalledWith('/deliveries/order/11', {
      message: 'Nova verzija je spremna.',
      fileUrl: 'https://example.com/file.zip',
      fileName: 'file.zip',
    });

    const detail = render(<DeliveryDetailModal orderId={11} version={1} onClose={onClose} />, { router: false });
    await flushPromises();
    expect(detail.text()).toContain('logo.zip');

    authState.value = {
      ...authState.value,
      user: { id: 99, username: 'admin', email: 'admin@example.com', role: 'ADMIN' },
    };
    const admin = render(<AdminOrderTools order={order} onChange={onChange} />, { router: false });
    click(admin.container.querySelector('button'));
    input(admin.container.querySelector('input[type="number"]'), '175');
    await submit(admin.container.querySelector('form'));
    expect(api.patch).toHaveBeenCalledWith('/orders/11', expect.arrayContaining([
      { op: 'replace', path: '/totalCost', value: 175 },
    ]), expect.anything());
  });
});

describe('profile and edit workflows', () => {
  it('renders profile settings and saves profile changes', async () => {
    authState.value = {
      ...authState.value,
      user: { id: 2, username: 'mila', email: 'mila@example.com', role: 'FREELANCER' },
    };
    const view = route(<ProfileSettingsPage />, '/dashboard/profile');
    await flushPromises();

    expect(view.text()).toContain('Moj profil');
    input(view.container.querySelector('textarea'), 'Azuriran bio');
    await submit(view.container.querySelector('form'));

    expect(api.patch).toHaveBeenCalledWith('/users/me', expect.objectContaining({ bio: 'Azuriran bio' }));
    expect(authState.value.setAuthenticatedUser).toHaveBeenCalled();
  });

  it('renders edit gig page and submits updated gig payload', async () => {
    const view = route(<EditGigPage />, '/dashboard/gigs/edit/:id', '/dashboard/gigs/edit/7');
    await flushPromises();

    expect(view.text()).toContain('Uredi uslugu');
    input(view.container.querySelector('input[name="title"]'), 'Profesionalni logo dizajn updated');
    await submit(view.container.querySelector('form'));

    expect(api.patch).toHaveBeenCalledWith('/gigs/7', expect.objectContaining({
      title: 'Profesionalni logo dizajn updated',
      categoryId: 3,
    }));
  });
});
