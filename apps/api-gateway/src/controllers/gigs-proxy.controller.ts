import { Controller, All, Req } from '@nestjs/common';
import { Request } from 'express';
import { ProxyService } from '../proxy/proxy.service';

const GIG_SERVICE_URL = () => process.env['GIG_SERVICE_URL'] || 'http://localhost:3002';

@Controller('gigs')
export class GigsProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(GIG_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(GIG_SERVICE_URL(), req);
  }
}

@Controller('categories')
export class CategoriesProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(GIG_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(GIG_SERVICE_URL(), req);
  }
}

@Controller('tags')
export class TagsProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(GIG_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(GIG_SERVICE_URL(), req);
  }
}
