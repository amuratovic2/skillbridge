import {
  Controller,
  Get,
  Post,
  Patch,
  Delete,
  Param,
  Body,
  ParseIntPipe,
} from '@nestjs/common';
import { CategoriesService } from './categories.service';

@Controller('categories')
export class CategoriesController {
  constructor(private readonly categoriesService: CategoriesService) {}

  @Get()
  async findAll() {
    const result = await this.categoriesService.findAll();
    return { success: true, data: result };
  }

  @Get(':slug')
  async findBySlug(@Param('slug') slug: string) {
    const result = await this.categoriesService.findBySlug(slug);
    return { success: true, data: result };
  }

  @Post()
  async create(@Body() body: { title: string }) {
    const result = await this.categoriesService.create(body.title);
    return { success: true, data: result };
  }

  @Patch(':id')
  async update(
    @Param('id', ParseIntPipe) id: number,
    @Body() body: { title: string },
  ) {
    const result = await this.categoriesService.update(id, body.title);
    return { success: true, data: result };
  }

  @Delete(':id')
  async delete(@Param('id', ParseIntPipe) id: number) {
    const result = await this.categoriesService.delete(id);
    return { success: true, data: result };
  }
}
