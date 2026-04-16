import {
  Controller,
  Get,
  Post,
  Patch,
  Param,
  Body,
  Query,
  Headers,
  ParseIntPipe,
} from '@nestjs/common';
import { DisputesService } from './disputes.service';

@Controller('disputes')
export class DisputesController {
  constructor(private readonly disputesService: DisputesService) {}

  @Post()
  async create(
    @Headers('x-user-id') userId: string,
    @Body() body: { orderId: number; reason: string; description?: string },
  ) {
    const result = await this.disputesService.create(parseInt(userId, 10), body);
    return { success: true, data: result };
  }

  @Get()
  async findAll(
    @Query('status') status?: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    const result = await this.disputesService.findAll(
      status,
      page ? parseInt(page, 10) : 1,
      limit ? parseInt(limit, 10) : 12,
    );
    return { success: true, ...result };
  }

  @Get(':id')
  async findById(@Param('id', ParseIntPipe) id: number) {
    const result = await this.disputesService.findById(id);
    return { success: true, data: result };
  }

  @Patch(':id/assign')
  async assign(
    @Param('id', ParseIntPipe) id: number,
    @Headers('x-user-id') adminId: string,
  ) {
    const result = await this.disputesService.assign(id, parseInt(adminId, 10));
    return { success: true, data: result };
  }

  @Patch(':id/resolve')
  async resolve(
    @Param('id', ParseIntPipe) id: number,
    @Headers('x-user-id') adminId: string,
    @Body() body: { resolution: string; status: 'RESOLVED_BUYER' | 'RESOLVED_SELLER' },
  ) {
    const result = await this.disputesService.resolve(id, parseInt(adminId, 10), body);
    return { success: true, data: result };
  }
}
