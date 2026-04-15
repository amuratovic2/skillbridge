import { Controller, All, Req } from '@nestjs/common';
import { Request } from 'express';
import { ProxyService } from '../proxy/proxy.service';

const USER_SERVICE_URL = () => process.env['USER_SERVICE_URL'] || 'http://localhost:3001';

@Controller('users')
export class UsersProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(USER_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(USER_SERVICE_URL(), req);
  }
}

@Controller('portfolios')
export class PortfoliosProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(USER_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(USER_SERVICE_URL(), req);
  }
}

@Controller('skills')
export class SkillsProxyController {
  constructor(private readonly proxyService: ProxyService) {}

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(USER_SERVICE_URL(), req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(USER_SERVICE_URL(), req);
  }
}
