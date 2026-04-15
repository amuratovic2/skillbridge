import { Module } from '@nestjs/common';
import { PrismaModule } from '../prisma/prisma.module';
import { GigsModule } from '../gigs/gigs.module';
import { CategoriesModule } from '../categories/categories.module';
import { TagsModule } from '../tags/tags.module';

@Module({
  imports: [PrismaModule, GigsModule, CategoriesModule, TagsModule],
})
export class AppModule {}
