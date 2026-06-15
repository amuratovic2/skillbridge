import { beforeEach, describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({
  get: vi.fn(),
  patch: vi.fn(),
  post: vi.fn(),
}));

vi.mock('./api', () => ({ default: api }));

import { communicationApi } from './communication';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('communicationApi', () => {
  it('unwraps conversations and paged messages with default params', async () => {
    const conversation = { partnerId: 2, lastMessage: 'Hej', lastAt: 'now', unreadCount: 1 };
    const message = { id: 1, senderId: 2, receiverId: 1, content: 'Hej', isRead: false, sentAt: 'now' };
    api.get.mockImplementation((url: string) =>
      Promise.resolve(url.includes('conversations')
        ? { data: { data: [conversation] } }
        : { data: { data: [message], meta: { total: 1, page: 1, limit: 20, totalPages: 1 } } }),
    );

    await expect(communicationApi.conversations()).resolves.toEqual([conversation]);
    await expect(communicationApi.conversation(2, { limit: 20 })).resolves.toEqual({
      data: [message],
      meta: { total: 1, page: 1, limit: 20, totalPages: 1 },
    });

    expect(api.get).toHaveBeenCalledWith('/messages/conversations');
    expect(api.get).toHaveBeenCalledWith('/messages/conversation/2', { params: { page: 1, limit: 20 } });
  });

  it('sends messages and marks partner messages as read', async () => {
    const message = { id: 1, senderId: 1, receiverId: 2, content: 'Zdravo', isRead: false, sentAt: 'now' };
    api.post.mockResolvedValue({ data: { data: message } });
    api.patch.mockResolvedValue({ data: { data: { updated: 3 } } });

    await expect(communicationApi.send({ receiverId: 2, content: 'Zdravo', orderId: 11 })).resolves.toEqual(message);
    await expect(communicationApi.markRead(2)).resolves.toEqual({ updated: 3 });

    expect(api.post).toHaveBeenCalledWith('/messages', { receiverId: 2, content: 'Zdravo', orderId: 11 });
    expect(api.patch).toHaveBeenCalledWith('/messages/read/2');
  });
});
