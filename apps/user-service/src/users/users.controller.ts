import {
  Controller,
  Get,
  Patch,
  Delete,
  Param,
  Body,
  Query,
  Headers,
  ParseIntPipe,
} from '@nestjs/common';
import { UsersService } from './users.service';

@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get()
  async findAll(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    const result = await this.usersService.findAll(
      page ? parseInt(page, 10) : 1,
      limit ? parseInt(limit, 10) : 12,
    );
    return { success: true, ...result };
  }

  @Get('me')
  async getMe(@Headers('x-user-id') userId: string) {
    const result = await this.usersService.findById(parseInt(userId, 10));
    return { success: true, data: result };
  }

  @Get(':id')
  async findById(@Param('id', ParseIntPipe) id: number) {
    const result = await this.usersService.getPublicProfile(id);
    return { success: true, data: result };
  }

  @Patch('me')
  async update(
    @Headers('x-user-id') userId: string,
    @Body() body: {
      firstName?: string;
      lastName?: string;
      bio?: string;
      profilePicture?: string;
      country?: string;
    },
  ) {
    const result = await this.usersService.update(parseInt(userId, 10), body);
    return { success: true, data: result };
  }

  @Delete('me')
  async deactivate(@Headers('x-user-id') userId: string) {
    const result = await this.usersService.deactivate(parseInt(userId, 10));
    return { success: true, data: result };
  }
}
