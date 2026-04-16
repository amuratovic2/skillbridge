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
import { MessagesService } from './messages.service';

@Controller('messages')
export class MessagesController {
  constructor(private readonly messagesService: MessagesService) {}

  @Post()
  async send(
    @Headers('x-user-id') userId: string,
    @Body() body: { receiverId: number; orderId?: number; content: string },
  ) {
    const result = await this.messagesService.send(parseInt(userId, 10), body);
    return { success: true, data: result };
  }

  @Get('conversations')
  async getConversationList(@Headers('x-user-id') userId: string) {
    const result = await this.messagesService.getConversationList(parseInt(userId, 10));
    return { success: true, data: result };
  }

  @Get('conversation/:otherUserId')
  async getConversation(
    @Headers('x-user-id') userId: string,
    @Param('otherUserId', ParseIntPipe) otherUserId: number,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    const result = await this.messagesService.getConversation(
      parseInt(userId, 10),
      otherUserId,
      page ? parseInt(page, 10) : 1,
      limit ? parseInt(limit, 10) : 50,
    );
    return { success: true, ...result };
  }

  @Get('order/:orderId')
  async getByOrder(
    @Param('orderId', ParseIntPipe) orderId: number,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    const result = await this.messagesService.getConversationsByOrder(
      orderId,
      page ? parseInt(page, 10) : 1,
      limit ? parseInt(limit, 10) : 50,
    );
    return { success: true, ...result };
  }

  @Patch('read/:senderId')
  async markAsRead(
    @Headers('x-user-id') userId: string,
    @Param('senderId', ParseIntPipe) senderId: number,
  ) {
    const result = await this.messagesService.markAsRead(parseInt(userId, 10), senderId);
    return { success: true, data: result };
  }
}
