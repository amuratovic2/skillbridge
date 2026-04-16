import { Injectable, NotFoundException, ForbiddenException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class DisputesService {
  constructor(private prisma: PrismaService) {}

  async create(initiatorId: number, data: {
    orderId: number;
    reason: string;
    description?: string;
  }) {
    const existing = await this.prisma.dispute.findFirst({
      where: { orderId: data.orderId, status: { in: ['OPEN', 'UNDER_REVIEW'] } },
    });
    if (existing) {
      throw new BadRequestException('An active dispute already exists for this order');
    }

    return this.prisma.dispute.create({
      data: { initiatorId, ...data },
    });
  }

  async findById(id: number) {
    const dispute = await this.prisma.dispute.findUnique({ where: { id } });
    if (!dispute) throw new NotFoundException('Dispute not found');
    return dispute;
  }

  async findAll(status?: string, page = 1, limit = 12) {
    const skip = (page - 1) * limit;
    const where = status ? { status: status as never } : {};

    const [disputes, total] = await Promise.all([
      this.prisma.dispute.findMany({
        where,
        skip,
        take: limit,
        orderBy: { createdAt: 'desc' },
      }),
      this.prisma.dispute.count({ where }),
    ]);

    return {
      data: disputes,
      meta: { total, page, limit, totalPages: Math.ceil(total / limit) },
    };
  }

  async assign(id: number, adminId: number) {
    const dispute = await this.findById(id);
    if (dispute.status !== 'OPEN') {
      throw new BadRequestException('Can only assign open disputes');
    }

    return this.prisma.dispute.update({
      where: { id },
      data: { adminId, status: 'UNDER_REVIEW' },
    });
  }

  async resolve(
    id: number,
    adminId: number,
    data: { resolution: string; status: 'RESOLVED_BUYER' | 'RESOLVED_SELLER' },
  ) {
    const dispute = await this.findById(id);
    if (dispute.adminId !== adminId) {
      throw new ForbiddenException('Only the assigned admin can resolve this dispute');
    }
    if (!['OPEN', 'UNDER_REVIEW'].includes(dispute.status)) {
      throw new BadRequestException('Dispute is not in a resolvable state');
    }

    return this.prisma.dispute.update({
      where: { id },
      data: {
        resolution: data.resolution,
        status: data.status,
        resolvedAt: new Date(),
      },
    });
  }
}
