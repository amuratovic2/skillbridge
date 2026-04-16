import {
  Controller,
  Get,
  Post,
  Patch,
  Delete,
  Param,
  Body,
  Headers,
  ParseIntPipe,
} from '@nestjs/common';
import { PortfolioService } from './portfolio.service';

@Controller('portfolios')
export class PortfolioController {
  constructor(private readonly portfolioService: PortfolioService) {}

  @Get('user/:userId')
  async findByUserId(@Param('userId', ParseIntPipe) userId: number) {
    const result = await this.portfolioService.findByUserId(userId);
    return { success: true, data: result };
  }

  @Post()
  async create(
    @Headers('x-user-id') userId: string,
    @Body() body: { title: string; description?: string; imageUrl?: string },
  ) {
    const result = await this.portfolioService.create(parseInt(userId, 10), body);
    return { success: true, data: result };
  }

  @Patch(':id')
  async update(
    @Param('id', ParseIntPipe) id: number,
    @Headers('x-user-id') userId: string,
    @Body() body: { title?: string; description?: string; imageUrl?: string },
  ) {
    const result = await this.portfolioService.update(id, parseInt(userId, 10), body);
    return { success: true, data: result };
  }

  @Delete(':id')
  async delete(
    @Param('id', ParseIntPipe) id: number,
    @Headers('x-user-id') userId: string,
  ) {
    const result = await this.portfolioService.delete(id, parseInt(userId, 10));
    return { success: true, data: result };
  }
}
