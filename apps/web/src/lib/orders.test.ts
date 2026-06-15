import { beforeEach, describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({
  delete: vi.fn(),
  get: vi.fn(),
  patch: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));

vi.mock('./api', () => ({ default: api }));

import {
  CUSTOM_OFFER_STATUS_META,
  ORDER_STATUS_META,
  customOffersApi,
  deliveriesApi,
  notificationsApi,
  ordersApi,
  type Order,
} from './orders';

const order: Order = {
  id: 11,
  clientId: 1,
  gigId: 7,
  sellerId: 2,
  totalCost: 250,
  status: 'PENDING',
  orderDate: '2026-06-01T00:00:00Z',
  deliveryDeadline: '2026-06-07T00:00:00Z',
  maxRevisions: 2,
  usedRevisions: 0,
  completedAt: null,
  cancelledAt: null,
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe('ordersApi', () => {
  it('unwraps list endpoints and preserves paging metadata', async () => {
    api.get.mockResolvedValue({ data: { data: [order], meta: { total: 1, page: 2 } } });

    await expect(ordersApi.buying({ page: 2, limit: 5 })).resolves.toEqual({
      data: [order],
      meta: { total: 1, page: 2 },
    });
    expect(api.get).toHaveBeenCalledWith('/orders/my/buying', { params: { page: 2, limit: 5 } });

    await expect(ordersApi.selling()).resolves.toEqual({ data: [order], meta: { total: 1, page: 2 } });
    expect(api.get).toHaveBeenLastCalledWith('/orders/my/selling', { params: undefined });
  });

  it('maps order read, statistics, revenue, status, patch and batch calls', async () => {
    api.get.mockImplementation((url: string) => {
      if (url.includes('statistics')) return Promise.resolve({ data: { data: { PENDING: 3 } } });
      if (url.includes('revenue')) return Promise.resolve({ data: { data: 735.25 } });
      return Promise.resolve({ data: { data: [order] } });
    });
    api.post.mockResolvedValue({ data: { data: order } });
    api.patch.mockResolvedValue({ data: { data: order } });

    await expect(ordersApi.byBuyingStatus('PENDING')).resolves.toEqual([order]);
    await expect(ordersApi.overdue()).resolves.toEqual([order]);
    await expect(ordersApi.statistics()).resolves.toEqual({ PENDING: 3 });
    await expect(ordersApi.revenue()).resolves.toBe(735.25);
    await expect(ordersApi.create(7, '  brief  ')).resolves.toEqual(order);
    await expect(ordersApi.updateStatus(11, 'ACCEPTED', 'ok')).resolves.toEqual(order);
    await expect(ordersApi.requestRevision(11, 'revise')).resolves.toEqual(order);
    await expect(ordersApi.patch(11, [{ op: 'replace', path: '/totalCost', value: 100 }])).resolves.toEqual(order);
    await expect(ordersApi.batchCreate([7, 8])).resolves.toEqual(order);

    expect(api.post).toHaveBeenCalledWith('/orders', { gigId: 7, requirements: 'brief' });
    expect(api.patch).toHaveBeenCalledWith('/orders/11/status', { status: 'ACCEPTED', note: 'ok' });
    expect(api.post).toHaveBeenCalledWith('/orders/11/revision', { message: 'revise' });
    expect(api.patch).toHaveBeenCalledWith(
      '/orders/11',
      [{ op: 'replace', path: '/totalCost', value: 100 }],
      { headers: { 'Content-Type': 'application/json;charset=UTF-8' } },
    );
    expect(api.post).toHaveBeenCalledWith('/orders/batch', { orders: [{ gigId: 7 }, { gigId: 8 }] });
  });

  it('exposes status metadata used by UI badges', () => {
    expect(ORDER_STATUS_META.COMPLETED.label).toBeTruthy();
    expect(CUSTOM_OFFER_STATUS_META.ACCEPTED.chip).toContain('green');
  });
});

describe('deliveriesApi', () => {
  it('wraps delivery endpoints', async () => {
    const delivery = { id: 1, orderId: 11, versionNumber: 1, message: null, fileUrl: null, fileName: null, createdAt: 'now' };
    api.get.mockResolvedValue({ data: { data: [delivery] } });
    api.post.mockResolvedValue({ data: { data: delivery } });

    await expect(deliveriesApi.forOrder(11)).resolves.toEqual([delivery]);
    await expect(deliveriesApi.create(11, { message: 'done' })).resolves.toEqual(delivery);
    api.get.mockResolvedValueOnce({ data: { data: delivery } });
    await expect(deliveriesApi.byVersion(11, 2)).resolves.toEqual(delivery);

    expect(api.get).toHaveBeenCalledWith('/deliveries/order/11');
    expect(api.post).toHaveBeenCalledWith('/deliveries/order/11', { message: 'done' });
    expect(api.get).toHaveBeenLastCalledWith('/deliveries/order/11/version/2');
  });
});

describe('customOffersApi and notificationsApi', () => {
  it('wraps custom offer lifecycle calls', async () => {
    const offer = {
      id: 1,
      gigId: null,
      orderId: null,
      senderId: 2,
      receiverId: 1,
      title: 'Offer',
      description: null,
      price: 90,
      deliveryDays: 5,
      revisionCount: 1,
      status: 'PENDING' as const,
      expiresAt: null,
      createdAt: 'now',
    };
    api.get.mockResolvedValue({ data: { data: [offer] } });
    api.post.mockResolvedValue({ data: { data: offer } });
    api.patch.mockResolvedValue({ data: { data: offer } });

    await expect(customOffersApi.received()).resolves.toEqual([offer]);
    await expect(customOffersApi.sent()).resolves.toEqual([offer]);
    await expect(customOffersApi.create({ receiverId: 1, title: 'Offer', price: 90, deliveryDays: 5, revisionCount: 1 })).resolves.toEqual(offer);
    await expect(customOffersApi.respond(1, 'ACCEPTED')).resolves.toEqual(offer);
    await expect(customOffersApi.withdraw(1)).resolves.toEqual(offer);

    expect(api.patch).toHaveBeenCalledWith('/custom-offers/1/respond', { status: 'ACCEPTED' });
    expect(api.patch).toHaveBeenCalledWith('/custom-offers/1/withdraw');
  });

  it('wraps notification list, count and read calls', async () => {
    api.get.mockImplementation((url: string) =>
      Promise.resolve(url.includes('unread-count')
        ? { data: { data: { count: 4 } } }
        : { data: { data: [{ id: 1, title: 'New', isRead: false }], meta: { total: 1 } } }),
    );
    api.patch.mockResolvedValue({ data: {} });

    await expect(notificationsApi.list({ limit: 10 })).resolves.toEqual({
      data: [{ id: 1, title: 'New', isRead: false }],
      meta: { total: 1 },
    });
    await expect(notificationsApi.unreadCount()).resolves.toBe(4);
    await notificationsApi.markRead(1);
    await notificationsApi.markAllRead();

    expect(api.get).toHaveBeenCalledWith('/notifications', { params: { limit: 10 } });
    expect(api.patch).toHaveBeenCalledWith('/notifications/1/read');
    expect(api.patch).toHaveBeenCalledWith('/notifications/read-all');
  });
});
