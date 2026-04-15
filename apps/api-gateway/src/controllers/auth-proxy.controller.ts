import { Controller, All, Req } from '@nestjs/common';
import { Request } from 'express';
import { ProxyService } from '../proxy/proxy.service';

@Controller('auth')
export class AuthProxyController {
  private readonly serviceUrl: string;

  constructor(private readonly proxyService: ProxyService) {
    this.serviceUrl = process.env['USER_SERVICE_URL'] || 'http://localhost:3001';
  }

  @All()
  async proxyRoot(@Req() req: Request) {
    return this.proxyService.forward(this.serviceUrl, req);
  }

  @All('*path')
  async proxy(@Req() req: Request) {
    return this.proxyService.forward(this.serviceUrl, req);
  }
}
