import {
  Controller,
  Get,
  Post,
  Patch,
  Delete,
  Param,
  Body,
  Query,
  Headers,
  ParseIntPipe,
} from '@nestjs/common';
import { GigsService } from './gigs.service';

@Controller('gigs')
export class GigsController {
  constructor(private readonly gigsService: GigsService) {}

  @Post()
  async create(
    @Headers('x-user-id') userId: string,
    @Body()
    body: {
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
    const result = await this.gigsService.create(parseInt(userId, 10), body);
    return { success: true, data: result };
  }

  @Get('search')
  async search(
    @Query('q') q?: string,
    @Query('categoryId') categoryId?: string,
    @Query('minPrice') minPrice?: string,
    @Query('maxPrice') maxPrice?: string,
    @Query('deliveryTime') deliveryTime?: string,
    @Query('sortBy') sortBy?: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    const result = await this.gigsService.search({
      q,
      categoryId: categoryId ? parseInt(categoryId, 10) : undefined,
      minPrice: minPrice ? parseFloat(minPrice) : undefined,
      maxPrice: maxPrice ? parseFloat(maxPrice) : undefined,
      deliveryTime: deliveryTime ? parseInt(deliveryTime, 10) : undefined,
      sortBy,
      page: page ? parseInt(page, 10) : 1,
      limit: limit ? parseInt(limit, 10) : 12,
    });
    return { success: true, ...result };
  }

  @Get('featured')
  async getFeatured(@Query('limit') limit?: string) {
    const result = await this.gigsService.getFeatured(
      limit ? parseInt(limit, 10) : 6,
    );
    return { success: true, data: result };
  }

  @Get('freelancer/:freelancerId')
  async findByFreelancer(
    @Param('freelancerId', ParseIntPipe) freelancerId: number,
  ) {
    const result = await this.gigsService.findByFreelancerId(freelancerId);
    return { success: true, data: result };
  }

  @Get(':id')
  async findById(@Param('id', ParseIntPipe) id: number) {
    const result = await this.gigsService.findById(id);
    return { success: true, data: result };
  }

  @Patch(':id')
  async update(
    @Param('id', ParseIntPipe) id: number,
    @Headers('x-user-id') userId: string,
    @Body() body: {
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
    const result = await this.gigsService.update(id, parseInt(userId, 10), body);
    return { success: true, data: result };
  }

  @Delete(':id')
  async delete(
    @Param('id', ParseIntPipe) id: number,
    @Headers('x-user-id') userId: string,
  ) {
    const result = await this.gigsService.delete(id, parseInt(userId, 10));
    return { success: true, data: result };
  }
}
