import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class MessagesService {
  constructor(private prisma: PrismaService) {}

  async send(senderId: number, data: { receiverId: number; orderId?: number; content: string }) {
    return this.prisma.message.create({
      data: { senderId, ...data },
    });
  }

  async getConversation(userId: number, otherUserId: number, page = 1, limit = 50) {
    const skip = (page - 1) * limit;

    const [messages, total] = await Promise.all([
      this.prisma.message.findMany({
        where: {
          OR: [
            { senderId: userId, receiverId: otherUserId },
            { senderId: otherUserId, receiverId: userId },
          ],
        },
        skip,
        take: limit,
        orderBy: { sentAt: 'desc' },
      }),
      this.prisma.message.count({
        where: {
          OR: [
            { senderId: userId, receiverId: otherUserId },
            { senderId: otherUserId, receiverId: userId },
          ],
        },
      }),
    ]);

    return {
      data: messages.reverse(),
      meta: { total, page, limit, totalPages: Math.ceil(total / limit) },
    };
  }

  async getConversationsByOrder(orderId: number, page = 1, limit = 50) {
    const skip = (page - 1) * limit;

    const [messages, total] = await Promise.all([
      this.prisma.message.findMany({
        where: { orderId },
        skip,
        take: limit,
        orderBy: { sentAt: 'desc' },
      }),
      this.prisma.message.count({ where: { orderId } }),
    ]);

    return {
      data: messages.reverse(),
      meta: { total, page, limit, totalPages: Math.ceil(total / limit) },
    };
  }

  async getConversationList(userId: number) {
    const sentMessages = await this.prisma.message.findMany({
      where: { senderId: userId },
      distinct: ['receiverId'],
      orderBy: { sentAt: 'desc' },
      select: { receiverId: true, sentAt: true, content: true },
    });

    const receivedMessages = await this.prisma.message.findMany({
      where: { receiverId: userId },
      distinct: ['senderId'],
      orderBy: { sentAt: 'desc' },
      select: { senderId: true, sentAt: true, content: true },
    });

    const conversationPartners = new Map<number, { lastMessage: string; lastAt: Date }>();

    for (const msg of sentMessages) {
      const existing = conversationPartners.get(msg.receiverId);
      if (!existing || msg.sentAt > existing.lastAt) {
        conversationPartners.set(msg.receiverId, { lastMessage: msg.content, lastAt: msg.sentAt });
      }
    }

    for (const msg of receivedMessages) {
      const existing = conversationPartners.get(msg.senderId);
      if (!existing || msg.sentAt > existing.lastAt) {
        conversationPartners.set(msg.senderId, { lastMessage: msg.content, lastAt: msg.sentAt });
      }
    }

    const unreadCounts = await this.prisma.message.groupBy({
      by: ['senderId'],
      where: { receiverId: userId, isRead: false },
      _count: true,
    });

    const unreadMap = new Map(unreadCounts.map((u) => [u.senderId, u._count]));

    return Array.from(conversationPartners.entries())
      .map(([partnerId, info]) => ({
        partnerId,
        lastMessage: info.lastMessage,
        lastAt: info.lastAt,
        unreadCount: unreadMap.get(partnerId) || 0,
      }))
      .sort((a, b) => b.lastAt.getTime() - a.lastAt.getTime());
  }

  async markAsRead(userId: number, senderId: number) {
    await this.prisma.message.updateMany({
      where: { senderId, receiverId: userId, isRead: false },
      data: { isRead: true },
    });
    return { message: 'Messages marked as read' };
  }
}
