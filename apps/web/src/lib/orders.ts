import api from './api';

export type OrderStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'IN_PROGRESS'
  | 'DELIVERED'
  | 'REVISION_REQUESTED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'DISPUTED';

export interface OrderHistoryEntry {
  id: number;
  changedByUserId: number;
  actionType: string;
  oldStatus: string | null;
  newStatus: string;
  note: string | null;
  changedAt: string;
}

export interface Order {
  id: number;
  clientId: number;
  gigId: number;
  sellerId: number;
  totalCost: number;
  status: OrderStatus;
  orderDate: string;
  deliveryDeadline: string | null;
  maxRevisions: number;
  usedRevisions: number;
  completedAt: string | null;
  cancelledAt: string | null;
  requirements?: string | null;
  history?: OrderHistoryEntry[];
}

export const ORDER_STATUS_META: Record<OrderStatus, { label: string; chip: string; bar: string }> = {
  PENDING: { label: 'Na čekanju', chip: 'bg-yellow-100 text-yellow-700', bar: 'bg-yellow-400' },
  ACCEPTED: { label: 'Prihvaćeno', chip: 'bg-blue-100 text-blue-700', bar: 'bg-blue-500' },
  IN_PROGRESS: { label: 'U izradi', chip: 'bg-blue-100 text-blue-700', bar: 'bg-blue-500' },
  DELIVERED: { label: 'Isporučeno', chip: 'bg-green-100 text-green-700', bar: 'bg-green-400' },
  REVISION_REQUESTED: { label: 'Na reviziji', chip: 'bg-orange-100 text-orange-700', bar: 'bg-orange-400' },
  COMPLETED: { label: 'Završeno', chip: 'bg-green-100 text-green-700', bar: 'bg-green-500' },
  CANCELLED: { label: 'Otkazano', chip: 'bg-red-100 text-red-700', bar: 'bg-red-400' },
  DISPUTED: { label: 'Spor', chip: 'bg-red-100 text-red-700', bar: 'bg-red-500' },
};

export interface PagedResponse<T> {
  data: T[];
  meta: { total: number; page: number; limit: number; totalPages: number };
}

const unwrap = <T>(payload: any): T => payload.data;

export const ordersApi = {
  buying(params?: { page?: number; limit?: number }) {
    return api
      .get('/orders/my/buying', { params })
      .then((r) => ({ data: unwrap<Order[]>(r.data) ?? [], meta: r.data.meta }));
  },
  selling(params?: { page?: number; limit?: number }) {
    return api
      .get('/orders/my/selling', { params })
      .then((r) => ({ data: unwrap<Order[]>(r.data) ?? [], meta: r.data.meta }));
  },
  byBuyingStatus(status: OrderStatus) {
    return api
      .get(`/orders/my/buying/status/${status}`)
      .then((r) => unwrap<Order[]>(r.data) ?? []);
  },
  byId(id: number | string) {
    return api.get(`/orders/${id}`).then((r) => unwrap<Order>(r.data));
  },
  overdue() {
    return api.get('/orders/overdue').then((r) => unwrap<Order[]>(r.data) ?? []);
  },
  statistics() {
    return api
      .get('/orders/statistics/by-status')
      .then((r) => (unwrap<Record<OrderStatus, number>>(r.data) ?? ({} as Record<OrderStatus, number>)));
  },
  revenue() {
    return api.get('/orders/my/revenue').then((r) => Number(unwrap<number>(r.data) ?? 0));
  },
  create(gigId: number, requirements?: string) {
    return api
      .post('/orders', { gigId, requirements: requirements?.trim() || undefined })
      .then((r) => unwrap<Order>(r.data));
  },
  updateStatus(id: number, status: OrderStatus, note?: string) {
    return api
      .patch(`/orders/${id}/status`, { status, note })
      .then((r) => unwrap<Order>(r.data));
  },
  requestRevision(id: number, message?: string) {
    return api.post(`/orders/${id}/revision`, { message }).then((r) => unwrap<Order>(r.data));
  },
  patch(id: number, ops: Array<{ op: 'replace' | 'add' | 'test'; path: string; value: unknown }>) {
    return api
      .patch(`/orders/${id}`, ops, {
        headers: { 'Content-Type': 'application/json;charset=UTF-8' },
      })
      .then((r) => unwrap<Order>(r.data));
  },
  batchCreate(gigIds: number[]) {
    return api
      .post('/orders/batch', { orders: gigIds.map((gigId) => ({ gigId })) })
      .then((r) => unwrap<Order[]>(r.data) ?? []);
  },
};

export interface Delivery {
  id: number;
  orderId: number;
  versionNumber: number;
  message: string | null;
  fileUrl: string | null;
  fileName: string | null;
  createdAt: string;
}

export const deliveriesApi = {
  forOrder(orderId: number | string) {
    return api
      .get(`/deliveries/order/${orderId}`)
      .then((r) => unwrap<Delivery[]>(r.data) ?? []);
  },
  create(orderId: number, body: { message?: string; fileUrl?: string; fileName?: string }) {
    return api
      .post(`/deliveries/order/${orderId}`, body)
      .then((r) => unwrap<Delivery>(r.data));
  },
  byVersion(orderId: number, version: number) {
    return api
      .get(`/deliveries/order/${orderId}/version/${version}`)
      .then((r) => unwrap<Delivery>(r.data));
  },
};

export type CustomOfferStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'EXPIRED';

export interface CustomOffer {
  id: number;
  gigId: number | null;
  orderId: number | null;
  senderId: number;
  receiverId: number;
  title: string;
  description: string | null;
  price: number;
  deliveryDays: number;
  revisionCount: number;
  status: CustomOfferStatus;
  expiresAt: string | null;
  createdAt: string;
}

export const CUSTOM_OFFER_STATUS_META: Record<CustomOfferStatus, { label: string; chip: string }> = {
  PENDING: { label: 'Na čekanju', chip: 'bg-yellow-100 text-yellow-700' },
  ACCEPTED: { label: 'Prihvaćeno', chip: 'bg-green-100 text-green-700' },
  REJECTED: { label: 'Odbijeno', chip: 'bg-red-100 text-red-700' },
  WITHDRAWN: { label: 'Povučeno', chip: 'bg-gray-100 text-gray-700' },
  EXPIRED: { label: 'Isteklo', chip: 'bg-gray-100 text-gray-700' },
};

export const customOffersApi = {
  received() {
    return api.get('/custom-offers/received').then((r) => unwrap<CustomOffer[]>(r.data) ?? []);
  },
  sent() {
    return api.get('/custom-offers/sent').then((r) => unwrap<CustomOffer[]>(r.data) ?? []);
  },
  create(payload: {
    receiverId: number;
    gigId?: number | null;
    title: string;
    description?: string;
    price: number;
    deliveryDays: number;
    revisionCount: number;
  }) {
    return api.post('/custom-offers', payload).then((r) => unwrap<CustomOffer>(r.data));
  },
  respond(id: number, status: 'ACCEPTED' | 'REJECTED') {
    return api
      .patch(`/custom-offers/${id}/respond`, { status })
      .then((r) => unwrap<CustomOffer>(r.data));
  },
  withdraw(id: number) {
    return api
      .patch(`/custom-offers/${id}/withdraw`)
      .then((r) => unwrap<CustomOffer>(r.data));
  },
};

export interface Notification {
  id: number;
  userId: number;
  type: string;
  title: string;
  content: string;
  referenceId: number | null;
  isRead: boolean;
  createdAt: string;
}

export const notificationsApi = {
  list(params?: { page?: number; limit?: number }) {
    return api
      .get('/notifications', { params })
      .then((r) => ({ data: unwrap<Notification[]>(r.data) ?? [], meta: r.data.meta }));
  },
  unreadCount() {
    return api
      .get('/notifications/unread-count')
      .then((r) => Number(unwrap<{ count: number }>(r.data)?.count ?? 0));
  },
  markRead(id: number) {
    return api.patch(`/notifications/${id}/read`);
  },
  markAllRead() {
    return api.patch('/notifications/read-all');
  },
};
