import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class PortfolioService {
  constructor(private prisma: PrismaService) {}

  async findByUserId(userId: number) {
    return this.prisma.portfolioItem.findMany({
      where: { userId },
      orderBy: { createdAt: 'desc' },
    });
  }

  async create(userId: number, data: { title: string; description?: string; imageUrl?: string }) {
    return this.prisma.portfolioItem.create({
      data: { ...data, userId },
    });
  }

  async update(
    id: number,
    userId: number,
    data: { title?: string; description?: string; imageUrl?: string },
  ) {
    const item = await this.findAndVerifyOwnership(id, userId);
    return this.prisma.portfolioItem.update({
      where: { id: item.id },
      data,
    });
  }

  async delete(id: number, userId: number) {
    await this.findAndVerifyOwnership(id, userId);
    await this.prisma.portfolioItem.delete({ where: { id } });
    return { message: 'Portfolio item deleted successfully' };
  }

  private async findAndVerifyOwnership(id: number, userId: number) {
    const item = await this.prisma.portfolioItem.findUnique({ where: { id } });
    if (!item) {
      throw new NotFoundException('Portfolio item not found');
    }
    if (item.userId !== userId) {
      throw new ForbiddenException('You can only modify your own portfolio items');
    }
    return item;
  }
}
