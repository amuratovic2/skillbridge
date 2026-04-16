import { Module } from '@nestjs/common';
import { PrismaModule } from '../prisma/prisma.module';
import { OrdersModule } from '../orders/orders.module';
import { DeliveriesModule } from '../deliveries/deliveries.module';
import { CustomOffersModule } from '../custom-offers/custom-offers.module';

@Module({
  imports: [PrismaModule, OrdersModule, DeliveriesModule, CustomOffersModule],
})
export class AppModule {}
