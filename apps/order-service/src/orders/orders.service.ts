import { Injectable, NotFoundException, BadRequestException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

const VALID_TRANSITIONS: Record<string, string[]> = {
  PENDING: ['ACCEPTED', 'CANCELLED'],
  ACCEPTED: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['DELIVERED', 'CANCELLED', 'DISPUTED'],
  DELIVERED: ['COMPLETED', 'REVISION_REQUESTED', 'DISPUTED'],
  REVISION_REQUESTED: ['IN_PROGRESS', 'CANCELLED', 'DISPUTED'],
  COMPLETED: [],
  CANCELLED: [],
  DISPUTED: ['COMPLETED', 'CANCELLED'],
};

@Injectable()
export class OrdersService {
  constructor(private prisma: PrismaService) {}

  async create(clientId: number, data: { gigId: number; totalCost: number; maxRevisions: number; deliveryDays: number }) {
    const deliveryDeadline = new Date();
    deliveryDeadline.setDate(deliveryDeadline.getDate() + data.deliveryDays);

    const order = await this.prisma.order.create({
      data: {
        clientId,
        gigId: data.gigId,
        totalCost: data.totalCost,
        maxRevisions: data.maxRevisions,
        deliveryDeadline,
      },
    });

    await this.addHistory(order.id, BigInt(clientId), 'ORDER_CREATED', '', 'PENDING');

    return order;
  }

  async findById(id: bigint) {
    const order = await this.prisma.order.findUnique({
      where: { id },
      include: {
        history: { orderBy: { changedAt: 'desc' } },
        deliveries: { orderBy: { versionNumber: 'desc' } },
      },
    });
    if (!order) throw new NotFoundException('Order not found');
    return this.serializeOrder(order);
  }

  async findByClient(clientId: number, page = 1, limit = 12) {
    const skip = (page - 1) * limit;
    const [orders, total] = await Promise.all([
      this.prisma.order.findMany({
        where: { clientId },
        skip,
        take: limit,
        orderBy: { orderDate: 'desc' },
      }),
      this.prisma.order.count({ where: { clientId } }),
    ]);

    return {
      data: orders.map((o) => this.serializeOrder(o)),
      meta: { total, page, limit, totalPages: Math.ceil(total / limit) },
    };
  }

  async findBySeller(sellerId: number, page = 1, limit = 12) {
    const skip = (page - 1) * limit;
    const [orders, total] = await Promise.all([
      this.prisma.order.findMany({
        skip,
        take: limit,
        orderBy: { orderDate: 'desc' },
      }),
      this.prisma.order.count(),
    ]);

    return {
      data: orders.map((o) => this.serializeOrder(o)),
      meta: { total, page, limit, totalPages: Math.ceil(total / limit) },
    };
  }

  async updateStatus(
    orderId: bigint,
    userId: number,
    newStatus: string,
    note?: string,
  ) {
    const order = await this.prisma.order.findUnique({ where: { id: orderId } });
    if (!order) throw new NotFoundException('Order not found');

    const currentStatus = order.status;
    const allowedTransitions = VALID_TRANSITIONS[currentStatus] || [];

    if (!allowedTransitions.includes(newStatus)) {
      throw new BadRequestException(
        `Cannot transition from ${currentStatus} to ${newStatus}`,
      );
    }

    const updateData: Record<string, unknown> = { status: newStatus as never };
    if (newStatus === 'COMPLETED') updateData['completedAt'] = new Date();
    if (newStatus === 'CANCELLED') updateData['cancelledAt'] = new Date();

    const updated = await this.prisma.order.update({
      where: { id: orderId },
      data: updateData as never,
    });

    await this.addHistory(orderId, BigInt(userId), 'STATUS_CHANGE', currentStatus, newStatus, note);

    return this.serializeOrder(updated);
  }

  async requestRevision(orderId: bigint, clientId: number, message: string) {
    const order = await this.prisma.order.findUnique({ where: { id: orderId } });
    if (!order) throw new NotFoundException('Order not found');
    if (order.clientId !== clientId) {
      throw new ForbiddenException('Only the client can request revisions');
    }
    if (order.status !== 'DELIVERED') {
      throw new BadRequestException('Can only request revision on delivered orders');
    }
    if (order.usedRevisions >= order.maxRevisions) {
      throw new BadRequestException('Maximum revisions reached');
    }

    const updated = await this.prisma.order.update({
      where: { id: orderId },
      data: {
        status: 'REVISION_REQUESTED',
        usedRevisions: { increment: 1 },
      },
    });

    await this.addHistory(orderId, BigInt(clientId), 'REVISION_REQUESTED', 'DELIVERED', 'REVISION_REQUESTED', message);

    return this.serializeOrder(updated);
  }

  private async addHistory(
    orderId: bigint,
    changedByUserId: bigint,
    actionType: string,
    oldStatus: string,
    newStatus: string,
    note?: string,
  ) {
    await this.prisma.orderHistory.create({
      data: { orderId, changedByUserId, actionType, oldStatus, newStatus, note },
    });
  }

  private serializeOrder(order: Record<string, unknown>) {
    return JSON.parse(
      JSON.stringify(order, (_key, value) =>
        typeof value === 'bigint' ? value.toString() : value,
      ),
    );
  }
}
