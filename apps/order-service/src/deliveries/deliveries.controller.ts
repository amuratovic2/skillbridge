import {
  Controller,
  Get,
  Post,
  Param,
  Body,
  Headers,
  ParseIntPipe,
} from '@nestjs/common';
import { DeliveriesService } from './deliveries.service';

@Controller('deliveries')
export class DeliveriesController {
  constructor(private readonly deliveriesService: DeliveriesService) {}

  @Post('order/:orderId')
  async create(
    @Param('orderId') orderId: string,
    @Headers('x-user-id') userId: string,
    @Body() body: { message?: string; fileUrl?: string; fileName?: string },
  ) {
    const result = await this.deliveriesService.create(
      BigInt(orderId),
      parseInt(userId, 10),
      body,
    );
    return { success: true, data: result };
  }

  @Get('order/:orderId')
  async findByOrderId(@Param('orderId') orderId: string) {
    const result = await this.deliveriesService.findByOrderId(BigInt(orderId));
    return { success: true, data: result };
  }

  @Get('order/:orderId/version/:version')
  async findByVersion(
    @Param('orderId') orderId: string,
    @Param('version', ParseIntPipe) version: number,
  ) {
    const result = await this.deliveriesService.findByVersion(BigInt(orderId), version);
    return { success: true, data: result };
  }
}
