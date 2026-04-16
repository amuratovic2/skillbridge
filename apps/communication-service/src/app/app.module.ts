import { Module } from '@nestjs/common';
import { PrismaModule } from '../prisma/prisma.module';
import { MessagesModule } from '../messages/messages.module';
import { ReviewsModule } from '../reviews/reviews.module';
import { DisputesModule } from '../disputes/disputes.module';
import { NotificationsModule } from '../notifications/notifications.module';

@Module({
  imports: [PrismaModule, MessagesModule, ReviewsModule, DisputesModule, NotificationsModule],
})
export class AppModule {}
