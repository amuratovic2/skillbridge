import { Injectable, NestMiddleware } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';
import axios from 'axios';

@Injectable()
export class AuthMiddleware implements NestMiddleware {
  private readonly userServiceUrl: string;

  constructor() {
    this.userServiceUrl = process.env['USER_SERVICE_URL'] || 'http://localhost:3001';
  }

  async use(req: Request, res: Response, next: NextFunction) {
    const authHeader = req.headers.authorization;
    if (!authHeader?.startsWith('Bearer ')) {
      return next();
    }

    const token = authHeader.split(' ')[1];

    try {
      const response = await axios.post(`${this.userServiceUrl}/api/auth/validate`, { token });
      const { userId, email, role } = response.data.data;

      req.headers['x-user-id'] = String(userId);
      req.headers['x-user-role'] = role;
      req.headers['x-user-email'] = email;
    } catch {
      // Token invalid — don't set headers, let downstream handle it
    }

    next();
  }
}
