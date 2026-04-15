import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class TagsService {
  constructor(private prisma: PrismaService) {}

  async findAll() {
    return this.prisma.tag.findMany({ orderBy: { name: 'asc' } });
  }

  async findPopular(limit = 20) {
    const tags = await this.prisma.tag.findMany({
      include: { _count: { select: { gigs: true } } },
      orderBy: { gigs: { _count: 'desc' } },
      take: limit,
    });

    return tags.map((tag) => ({
      id: tag.id,
      name: tag.name,
      slug: tag.slug,
      gigCount: tag._count.gigs,
    }));
  }
}
