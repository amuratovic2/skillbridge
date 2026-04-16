import { Module } from '@nestjs/common';
import { PrismaModule } from '../prisma/prisma.module';
import { AuthModule } from '../auth/auth.module';
import { UsersModule } from '../users/users.module';
import { PortfolioModule } from '../portfolio/portfolio.module';
import { SkillsModule } from '../skills/skills.module';

@Module({
  imports: [PrismaModule, AuthModule, UsersModule, PortfolioModule, SkillsModule],
})
export class AppModule {}
