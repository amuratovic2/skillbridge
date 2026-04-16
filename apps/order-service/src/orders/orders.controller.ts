import {
  Controller,
  Get,
  Post,
  Patch,
  Param,
  Body,
  Query,
  Headers,
  UnauthorizedException,
} from '@nestjs/common';
import { OrdersService } from './orders.service';

@Controller('orders')
export class OrdersController {
  constructor(private readonly ordersService: OrdersService) {}

  private getUserId(userId: string): number {
    const id = parseInt(userId, 10);
    if (!userId || isNaN(id)) {
      throw new UnauthorizedException('Authentication required');
    }
    return id;
  }

  @Post()
  async create(
    @Headers('x-user-id') userId: string,
    @Body() body: { gigId: number; totalCost: number; maxRevisions: number; deliveryDays: number },
  ) {
    const result = await this.ordersService.create(this.getUserId(userId), body);
    return { success: true, data: result };
  }

  @Get('my/buying')
  async getMyBuyingOrders(
    @Headers('x-user-id') userId: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    const result = await this.ordersService.findByClient(
      this.getUserId(userId),
      page ? parseInt(page, 10) : 1,
      limit ? parseInt(limit, 10) : 12,
    );
    return { success: true, ...result };
  }

  @Get('my/selling')
  async getMySellingOrders(
    @Headers('x-user-id') userId: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    const result = await this.ordersService.findBySeller(
      this.getUserId(userId),
      page ? parseInt(page, 10) : 1,
      limit ? parseInt(limit, 10) : 12,
    );
    return { success: true, ...result };
  }

  @Get(':id')
  async findById(@Param('id') id: string) {
    const result = await this.ordersService.findById(BigInt(id));
    return { success: true, data: result };
  }

  @Patch(':id/status')
  async updateStatus(
    @Param('id') id: string,
    @Headers('x-user-id') userId: string,
    @Body() body: { status: string; note?: string },
  ) {
    const result = await this.ordersService.updateStatus(
      BigInt(id),
      this.getUserId(userId),
      body.status,
      body.note,
    );
    return { success: true, data: result };
  }

  @Post(':id/revision')
  async requestRevision(
    @Param('id') id: string,
    @Headers('x-user-id') userId: string,
    @Body() body: { message: string },
  ) {
    const result = await this.ordersService.requestRevision(
      BigInt(id),
      this.getUserId(userId),
      body.message,
    );
    return { success: true, data: result };
  }
}
