import {
  Controller,
  Get,
  Patch,
  Param,
  Query,
  Headers,
  ParseIntPipe,
} from '@nestjs/common';
import { NotificationsService } from './notifications.service';

@Controller('notifications')
export class NotificationsController {
  constructor(private readonly notificationsService: NotificationsService) {}

  @Get()
  async findByUser(
    @Headers('x-user-id') userId: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    const result = await this.notificationsService.findByUser(
      parseInt(userId, 10),
      page ? parseInt(page, 10) : 1,
      limit ? parseInt(limit, 10) : 20,
    );
    return { success: true, ...result };
  }

  @Get('unread-count')
  async getUnreadCount(@Headers('x-user-id') userId: string) {
    const result = await this.notificationsService.getUnreadCount(parseInt(userId, 10));
    return { success: true, data: result };
  }

  @Patch(':id/read')
  async markAsRead(
    @Param('id', ParseIntPipe) id: number,
    @Headers('x-user-id') userId: string,
  ) {
    const result = await this.notificationsService.markAsRead(id, parseInt(userId, 10));
    return { success: true, data: result };
  }

  @Patch('read-all')
  async markAllAsRead(@Headers('x-user-id') userId: string) {
    const result = await this.notificationsService.markAllAsRead(parseInt(userId, 10));
    return { success: true, data: result };
  }
}
