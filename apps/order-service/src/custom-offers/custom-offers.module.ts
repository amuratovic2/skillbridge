import { Module } from '@nestjs/common';
import { CustomOffersController } from './custom-offers.controller';
import { CustomOffersService } from './custom-offers.service';

@Module({
  controllers: [CustomOffersController],
  providers: [CustomOffersService],
})
export class CustomOffersModule {}
