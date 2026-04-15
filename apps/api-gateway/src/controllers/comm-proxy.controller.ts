import { Controller, All, Req } from '@nestjs/common';
import { Request } from 'express';
import { ProxyService } from '../proxy/proxy.service';

const COMM_SERVICE_URL = () => process.env['COMM_SERVICE_URL'] || 'http://localhost:3004';

@Controller('messages')
export class MessagesProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(COMM_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(COMM_SERVICE_URL(), req);
  }
}

@Controller('reviews')
export class ReviewsProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(COMM_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(COMM_SERVICE_URL(), req);
  }
}

@Controller('disputes')
export class DisputesProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(COMM_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(COMM_SERVICE_URL(), req);
  }
}

@Controller('notifications')
export class NotificationsProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(COMM_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(COMM_SERVICE_URL(), req);
  }
}
