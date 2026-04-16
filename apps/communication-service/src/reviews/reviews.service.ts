import { Injectable, NotFoundException, ConflictException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class ReviewsService {
  constructor(private prisma: PrismaService) {}

  async create(reviewerId: number, data: {
    orderId: number;
    revieweeId: number;
    rating: number;
    comment?: string;
  }) {
    if (data.rating < 1 || data.rating > 5) {
      throw new BadRequestException('Rating must be between 1 and 5');
    }

    const existing = await this.prisma.review.findUnique({
      where: { orderId_reviewerId: { orderId: data.orderId, reviewerId } },
    });
    if (existing) {
      throw new ConflictException('You have already reviewed this order');
    }

    return this.prisma.review.create({
      data: { reviewerId, ...data },
    });
  }

  async findByReviewee(revieweeId: number, page = 1, limit = 12) {
    const skip = (page - 1) * limit;

    const [reviews, total] = await Promise.all([
      this.prisma.review.findMany({
        where: { revieweeId },
        skip,
        take: limit,
        orderBy: { createdAt: 'desc' },
      }),
      this.prisma.review.count({ where: { revieweeId } }),
    ]);

    return {
      data: reviews,
      meta: { total, page, limit, totalPages: Math.ceil(total / limit) },
    };
  }

  async findByOrder(orderId: number) {
    return this.prisma.review.findMany({ where: { orderId } });
  }

  async getAverageRating(revieweeId: number) {
    const result = await this.prisma.review.aggregate({
      where: { revieweeId },
      _avg: { rating: true },
      _count: { rating: true },
    });

    return {
      averageRating: result._avg.rating ? Math.round(result._avg.rating * 10) / 10 : 0,
      totalReviews: result._count.rating,
    };
  }
}
