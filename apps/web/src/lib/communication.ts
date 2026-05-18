import api from './api';

export interface ConversationSummary {
  partnerId: number;
  lastMessage: string;
  lastAt: string;
  unreadCount: number;
}

export interface Message {
  id: number;
  senderId: number;
  receiverId: number;
  orderId?: number | null;
  content: string;
  isRead: boolean;
  sentAt: string;
}

export interface MessagePageMeta {
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

const unwrap = <T>(payload: { data?: T }) => payload.data as T;

export const communicationApi = {
  conversations() {
    return api
      .get('/messages/conversations')
      .then((res) => unwrap<ConversationSummary[]>(res.data) ?? []);
  },

  conversation(otherUserId: number, params?: { page?: number; limit?: number }) {
    return api
      .get(`/messages/conversation/${otherUserId}`, {
        params: { page: params?.page ?? 1, limit: params?.limit ?? 50 },
      })
      .then((res) => ({
        data: unwrap<Message[]>(res.data) ?? [],
        meta: (res.data.meta ?? { total: 0, page: 1, limit: params?.limit ?? 50, totalPages: 1 }) as MessagePageMeta,
      }));
  },

  send(payload: { receiverId: number; content: string; orderId?: number }) {
    return api.post('/messages', payload).then((res) => unwrap<Message>(res.data));
  },

  markRead(senderId: number) {
    return api.patch(`/messages/read/${senderId}`).then((res) => unwrap<{ updated: number }>(res.data));
  },
};
