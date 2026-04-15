import { Controller, Get, Query } from '@nestjs/common';
import { TagsService } from './tags.service';

@Controller('tags')
export class TagsController {
  constructor(private readonly tagsService: TagsService) {}

  @Get()
  async findAll() {
    const result = await this.tagsService.findAll();
    return { success: true, data: result };
  }

  @Get('popular')
  async findPopular(@Query('limit') limit?: string) {
    const result = await this.tagsService.findPopular(
      limit ? parseInt(limit, 10) : 20,
    );
    return { success: true, data: result };
  }
}
