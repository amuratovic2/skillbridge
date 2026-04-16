import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class DeliveriesService {
  constructor(private prisma: PrismaService) {}

  async create(
    orderId: bigint,
    freelancerId: number,
    data: { message?: string; fileUrl?: string; fileName?: string },
  ) {
    const order = await this.prisma.order.findUnique({ where: { id: orderId } });
    if (!order) throw new NotFoundException('Order not found');

    if (!['IN_PROGRESS', 'REVISION_REQUESTED'].includes(order.status)) {
      throw new BadRequestException('Can only deliver on in-progress or revision-requested orders');
    }

    const lastDelivery = await this.prisma.delivery.findFirst({
      where: { orderId },
      orderBy: { versionNumber: 'desc' },
    });

    const versionNumber = (lastDelivery?.versionNumber ?? 0) + 1;

    const delivery = await this.prisma.delivery.create({
      data: {
        orderId,
        versionNumber,
        message: data.message,
        fileUrl: data.fileUrl,
        fileName: data.fileName,
      },
    });

    await this.prisma.order.update({
      where: { id: orderId },
      data: { status: 'DELIVERED' },
    });

    await this.prisma.orderHistory.create({
      data: {
        orderId,
        changedByUserId: BigInt(freelancerId),
        actionType: 'DELIVERY',
        oldStatus: order.status,
        newStatus: 'DELIVERED',
        note: `Version ${versionNumber} delivered`,
      },
    });

    return this.serialize(delivery);
  }

  async findByOrderId(orderId: bigint) {
    const deliveries = await this.prisma.delivery.findMany({
      where: { orderId },
      orderBy: { versionNumber: 'desc' },
    });
    return deliveries.map(this.serialize);
  }

  async findByVersion(orderId: bigint, versionNumber: number) {
    const delivery = await this.prisma.delivery.findFirst({
      where: { orderId, versionNumber },
    });
    if (!delivery) throw new NotFoundException('Delivery not found');
    return this.serialize(delivery);
  }

  private serialize(obj: Record<string, unknown>) {
    return JSON.parse(
      JSON.stringify(obj, (_key, value) =>
        typeof value === 'bigint' ? value.toString() : value,
      ),
    );
  }
}
