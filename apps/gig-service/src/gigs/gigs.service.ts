import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { Prisma } from '../generated/prisma-client';

@Injectable()
export class GigsService {
  constructor(private prisma: PrismaService) {}

  async create(
    freelancerId: number,
    data: {
      title: string;
      description?: string;
      categoryId: number;
      cost: number;
      deliveryTime: number;
      revisionCount: number;
      coverImage?: string;
      tags?: string[];
    },
  ) {
    const gig = await this.prisma.gig.create({
      data: {
        freelancerId,
        categoryId: data.categoryId,
        title: data.title,
        description: data.description,
        cost: data.cost,
        deliveryTime: data.deliveryTime,
        revisionCount: data.revisionCount,
        coverImage: data.coverImage,
      },
      include: { category: true, tags: { include: { tag: true } }, images: true },
    });

    if (data.tags?.length) {
      await this.syncTags(gig.id, data.tags);
    }

    return this.findById(gig.id);
  }

  async findById(id: number) {
    const gig = await this.prisma.gig.findUnique({
      where: { id },
      include: {
        category: true,
        tags: { include: { tag: true } },
        images: { orderBy: { sortOrder: 'asc' } },
      },
    });

    if (!gig) {
      throw new NotFoundException('Gig not found');
    }

    return {
      ...gig,
      tags: gig.tags.map((gt) => gt.tag),
    };
  }

  async update(
    id: number,
    freelancerId: number,
    data: {
      title?: string;
      description?: string;
      categoryId?: number;
      cost?: number;
      deliveryTime?: number;
      revisionCount?: number;
      coverImage?: string;
      status?: string;
      tags?: string[];
    },
  ) {
    const gig = await this.prisma.gig.findUnique({ where: { id } });
    if (!gig) throw new NotFoundException('Gig not found');
    if (gig.freelancerId !== freelancerId) {
      throw new ForbiddenException('You can only edit your own gigs');
    }

    const { tags, ...updateData } = data;

    await this.prisma.gig.update({
      where: { id },
      data: updateData as Prisma.GigUpdateInput,
    });

    if (tags) {
      await this.syncTags(id, tags);
    }

    return this.findById(id);
  }

  async delete(id: number, freelancerId: number) {
    const gig = await this.prisma.gig.findUnique({ where: { id } });
    if (!gig) throw new NotFoundException('Gig not found');
    if (gig.freelancerId !== freelancerId) {
      throw new ForbiddenException('You can only delete your own gigs');
    }

    await this.prisma.gig.update({ where: { id }, data: { status: 'DELETED' } });
    return { message: 'Gig deleted successfully' };
  }

  async search(params: {
    q?: string;
    categoryId?: number;
    minPrice?: number;
    maxPrice?: number;
    deliveryTime?: number;
    sortBy?: string;
    page?: number;
    limit?: number;
  }) {
    const page = params.page || 1;
    const limit = Math.min(params.limit || 12, 100);
    const skip = (page - 1) * limit;

    const where: Prisma.GigWhereInput = {
      status: 'ACTIVE',
    };

    if (params.q) {
      where.OR = [
        { title: { contains: params.q, mode: 'insensitive' } },
        { description: { contains: params.q, mode: 'insensitive' } },
      ];
    }

    if (params.categoryId) {
      where.categoryId = params.categoryId;
    }

    if (params.minPrice || params.maxPrice) {
      where.cost = {};
      if (params.minPrice) where.cost.gte = params.minPrice;
      if (params.maxPrice) where.cost.lte = params.maxPrice;
    }

    if (params.deliveryTime) {
      where.deliveryTime = { lte: params.deliveryTime };
    }

    let orderBy: Prisma.GigOrderByWithRelationInput = { createdAt: 'desc' };
    switch (params.sortBy) {
      case 'price_asc':
        orderBy = { cost: 'asc' };
        break;
      case 'price_desc':
        orderBy = { cost: 'desc' };
        break;
      case 'newest':
        orderBy = { createdAt: 'desc' };
        break;
    }

    const [gigs, total] = await Promise.all([
      this.prisma.gig.findMany({
        where,
        skip,
        take: limit,
        orderBy,
        include: {
          category: true,
          tags: { include: { tag: true } },
          images: { orderBy: { sortOrder: 'asc' }, take: 1 },
        },
      }),
      this.prisma.gig.count({ where }),
    ]);

    return {
      data: gigs.map((gig) => ({
        ...gig,
        tags: gig.tags.map((gt) => gt.tag),
      })),
      meta: { total, page, limit, totalPages: Math.ceil(total / limit) },
    };
  }

  async findByFreelancerId(freelancerId: number) {
    const gigs = await this.prisma.gig.findMany({
      where: { freelancerId, status: { not: 'DELETED' } },
      include: {
        category: true,
        tags: { include: { tag: true } },
      },
      orderBy: { createdAt: 'desc' },
    });

    return gigs.map((gig) => ({
      ...gig,
      tags: gig.tags.map((gt) => gt.tag),
    }));
  }

  async getFeatured(limit = 6) {
    const gigs = await this.prisma.gig.findMany({
      where: { status: 'ACTIVE' },
      take: limit,
      orderBy: { createdAt: 'desc' },
      include: {
        category: true,
        tags: { include: { tag: true } },
      },
    });

    return gigs.map((gig) => ({
      ...gig,
      tags: gig.tags.map((gt) => gt.tag),
    }));
  }

  private async syncTags(gigId: number, tagNames: string[]) {
    await this.prisma.gigTag.deleteMany({ where: { gigId } });

    for (const name of tagNames) {
      const slug = name.toLowerCase().replace(/\s+/g, '-');
      const tag = await this.prisma.tag.upsert({
        where: { slug },
        create: { name, slug },
        update: {},
      });
      await this.prisma.gigTag.create({ data: { gigId, tagId: tag.id } });
    }
  }
}
