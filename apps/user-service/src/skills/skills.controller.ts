import {
  Controller,
  Get,
  Post,
  Delete,
  Param,
  Body,
  Headers,
  ParseIntPipe,
} from '@nestjs/common';
import { SkillsService } from './skills.service';

@Controller('skills')
export class SkillsController {
  constructor(private readonly skillsService: SkillsService) {}

  @Get()
  async findAll() {
    const result = await this.skillsService.findAll();
    return { success: true, data: result };
  }

  @Post()
  async create(@Body() body: { name: string }) {
    const result = await this.skillsService.create(body.name);
    return { success: true, data: result };
  }

  @Get('user/:userId')
  async getUserSkills(@Param('userId', ParseIntPipe) userId: number) {
    const result = await this.skillsService.getUserSkills(userId);
    return { success: true, data: result };
  }

  @Post('me/:skillId')
  async addSkill(
    @Param('skillId', ParseIntPipe) skillId: number,
    @Headers('x-user-id') userId: string,
  ) {
    const result = await this.skillsService.addSkillToUser(parseInt(userId, 10), skillId);
    return { success: true, data: result };
  }

  @Delete('me/:skillId')
  async removeSkill(
    @Param('skillId', ParseIntPipe) skillId: number,
    @Headers('x-user-id') userId: string,
  ) {
    const result = await this.skillsService.removeSkillFromUser(parseInt(userId, 10), skillId);
    return { success: true, data: result };
  }
}
