import { Injectable, NotFoundException, ForbiddenException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class CustomOffersService {
  constructor(private prisma: PrismaService) {}

  async create(senderId: number, data: {
    gigId?: number;
    receiverId: number;
    title: string;
    description?: string;
    price: number;
    deliveryDays: number;
    revisionCount: number;
  }) {
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 7);

    const offer = await this.prisma.customOffer.create({
      data: { ...data, senderId, expiresAt },
    });

    return this.serialize(offer);
  }

  async findReceived(userId: number) {
    const offers = await this.prisma.customOffer.findMany({
      where: { receiverId: userId },
      orderBy: { createdAt: 'desc' },
    });
    return offers.map(this.serialize);
  }

  async findSent(userId: number) {
    const offers = await this.prisma.customOffer.findMany({
      where: { senderId: userId },
      orderBy: { createdAt: 'desc' },
    });
    return offers.map(this.serialize);
  }

  async respond(offerId: bigint, userId: number, status: 'ACCEPTED' | 'REJECTED') {
    const offer = await this.prisma.customOffer.findUnique({ where: { id: offerId } });
    if (!offer) throw new NotFoundException('Custom offer not found');

    if (offer.receiverId !== userId) {
      throw new ForbiddenException('Only the receiver can respond to this offer');
    }
    if (offer.status !== 'PENDING') {
      throw new BadRequestException('This offer has already been responded to');
    }
    if (offer.expiresAt && offer.expiresAt < new Date()) {
      throw new BadRequestException('This offer has expired');
    }

    const updated = await this.prisma.customOffer.update({
      where: { id: offerId },
      data: { status },
    });

    return this.serialize(updated);
  }

  async withdraw(offerId: bigint, senderId: number) {
    const offer = await this.prisma.customOffer.findUnique({ where: { id: offerId } });
    if (!offer) throw new NotFoundException('Custom offer not found');
    if (offer.senderId !== senderId) {
      throw new ForbiddenException('Only the sender can withdraw this offer');
    }
    if (offer.status !== 'PENDING') {
      throw new BadRequestException('Can only withdraw pending offers');
    }

    const updated = await this.prisma.customOffer.update({
      where: { id: offerId },
      data: { status: 'WITHDRAWN' },
    });

    return this.serialize(updated);
  }

  private serialize(obj: Record<string, unknown>) {
    return JSON.parse(
      JSON.stringify(obj, (_key, value) =>
        typeof value === 'bigint' ? value.toString() : value,
      ),
    );
  }
}
