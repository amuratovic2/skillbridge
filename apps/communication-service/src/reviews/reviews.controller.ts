import {
  Controller,
  Get,
  Post,
  Param,
  Body,
  Query,
  Headers,
  ParseIntPipe,
} from '@nestjs/common';
import { ReviewsService } from './reviews.service';

@Controller('reviews')
export class ReviewsController {
  constructor(private readonly reviewsService: ReviewsService) {}

  @Post()
  async create(
    @Headers('x-user-id') userId: string,
    @Body() body: { orderId: number; revieweeId: number; rating: number; comment?: string },
  ) {
    const result = await this.reviewsService.create(parseInt(userId, 10), body);
    return { success: true, data: result };
  }

  @Get('user/:userId')
  async findByUser(
    @Param('userId', ParseIntPipe) userId: number,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    const result = await this.reviewsService.findByReviewee(
      userId,
      page ? parseInt(page, 10) : 1,
      limit ? parseInt(limit, 10) : 12,
    );
    return { success: true, ...result };
  }

  @Get('order/:orderId')
  async findByOrder(@Param('orderId', ParseIntPipe) orderId: number) {
    const result = await this.reviewsService.findByOrder(orderId);
    return { success: true, data: result };
  }

  @Get('rating/:userId')
  async getAverageRating(@Param('userId', ParseIntPipe) userId: number) {
    const result = await this.reviewsService.getAverageRating(userId);
    return { success: true, data: result };
  }
}
