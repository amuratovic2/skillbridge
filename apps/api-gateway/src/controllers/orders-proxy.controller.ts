import { Controller, All, Req } from '@nestjs/common';
import { Request } from 'express';
import { ProxyService } from '../proxy/proxy.service';

const ORDER_SERVICE_URL = () => process.env['ORDER_SERVICE_URL'] || 'http://localhost:3003';

@Controller('orders')
export class OrdersProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(ORDER_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(ORDER_SERVICE_URL(), req);
  }
}

@Controller('deliveries')
export class DeliveriesProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(ORDER_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(ORDER_SERVICE_URL(), req);
  }
}

@Controller('custom-offers')
export class CustomOffersProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(ORDER_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(ORDER_SERVICE_URL(), req);
  }
}
