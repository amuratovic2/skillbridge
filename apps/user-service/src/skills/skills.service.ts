import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class SkillsService {
  constructor(private prisma: PrismaService) {}

  async findAll() {
    return this.prisma.skill.findMany({ orderBy: { name: 'asc' } });
  }

  async create(name: string) {
    const existing = await this.prisma.skill.findUnique({ where: { name } });
    if (existing) {
      throw new ConflictException('Skill already exists');
    }
    return this.prisma.skill.create({ data: { name } });
  }

  async addSkillToUser(userId: number, skillId: number) {
    const skill = await this.prisma.skill.findUnique({ where: { id: skillId } });
    if (!skill) {
      throw new NotFoundException('Skill not found');
    }

    const existing = await this.prisma.userSkill.findUnique({
      where: { userId_skillId: { userId, skillId } },
    });
    if (existing) {
      throw new ConflictException('Skill already added to user');
    }

    await this.prisma.userSkill.create({ data: { userId, skillId } });
    return { message: 'Skill added successfully' };
  }

  async removeSkillFromUser(userId: number, skillId: number) {
    await this.prisma.userSkill.deleteMany({ where: { userId, skillId } });
    return { message: 'Skill removed successfully' };
  }

  async getUserSkills(userId: number) {
    const userSkills = await this.prisma.userSkill.findMany({
      where: { userId },
      include: { skill: true },
    });
    return userSkills.map((us) => us.skill);
  }
}
