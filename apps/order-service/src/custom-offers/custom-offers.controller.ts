import {
  Controller,
  Get,
  Post,
  Patch,
  Param,
  Body,
  Headers,
} from '@nestjs/common';
import { CustomOffersService } from './custom-offers.service';

@Controller('custom-offers')
export class CustomOffersController {
  constructor(private readonly customOffersService: CustomOffersService) {}

  @Post()
  async create(
    @Headers('x-user-id') userId: string,
    @Body() body: {
      gigId?: number;
      receiverId: number;
      title: string;
      description?: string;
      price: number;
      deliveryDays: number;
      revisionCount: number;
    },
  ) {
    const result = await this.customOffersService.create(parseInt(userId, 10), body);
    return { success: true, data: result };
  }

  @Get('received')
  async getReceived(@Headers('x-user-id') userId: string) {
    const result = await this.customOffersService.findReceived(parseInt(userId, 10));
    return { success: true, data: result };
  }

  @Get('sent')
  async getSent(@Headers('x-user-id') userId: string) {
    const result = await this.customOffersService.findSent(parseInt(userId, 10));
    return { success: true, data: result };
  }

  @Patch(':id/respond')
  async respond(
    @Param('id') id: string,
    @Headers('x-user-id') userId: string,
    @Body() body: { status: 'ACCEPTED' | 'REJECTED' },
  ) {
    const result = await this.customOffersService.respond(
      BigInt(id),
      parseInt(userId, 10),
      body.status,
    );
    return { success: true, data: result };
  }

  @Patch(':id/withdraw')
  async withdraw(
    @Param('id') id: string,
    @Headers('x-user-id') userId: string,
  ) {
    const result = await this.customOffersService.withdraw(
      BigInt(id),
      parseInt(userId, 10),
    );
    return { success: true, data: result };
  }
}
