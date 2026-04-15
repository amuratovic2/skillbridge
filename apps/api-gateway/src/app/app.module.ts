import { Module, MiddlewareConsumer, NestModule } from '@nestjs/common';
import { ProxyModule } from '../proxy/proxy.module';
import { AuthMiddleware } from '../middleware/auth.middleware';
import { AuthProxyController } from '../controllers/auth-proxy.controller';
import {
  UsersProxyController,
  PortfoliosProxyController,
  SkillsProxyController,
} from '../controllers/users-proxy.controller';
import {
  GigsProxyController,
  CategoriesProxyController,
  TagsProxyController,
} from '../controllers/gigs-proxy.controller';
import {
  OrdersProxyController,
  DeliveriesProxyController,
  CustomOffersProxyController,
} from '../controllers/orders-proxy.controller';
import {
  MessagesProxyController,
  ReviewsProxyController,
  DisputesProxyController,
  NotificationsProxyController,
} from '../controllers/comm-proxy.controller';

@Module({
  imports: [ProxyModule],
  controllers: [
    AuthProxyController,
    UsersProxyController,
    PortfoliosProxyController,
    SkillsProxyController,
    GigsProxyController,
    CategoriesProxyController,
    TagsProxyController,
    OrdersProxyController,
    DeliveriesProxyController,
    CustomOffersProxyController,
    MessagesProxyController,
    ReviewsProxyController,
    DisputesProxyController,
    NotificationsProxyController,
  ],
})
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer) {
    consumer
      .apply(AuthMiddleware)
      .exclude('api/auth/(.*)')
      .forRoutes('*');
  }
}
