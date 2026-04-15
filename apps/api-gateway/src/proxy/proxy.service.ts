import { Injectable, HttpException } from '@nestjs/common';
import axios, { AxiosRequestConfig } from 'axios';
import { Request } from 'express';

@Injectable()
export class ProxyService {
  async forward(serviceUrl: string, req: Request) {
    const url = `${serviceUrl}${req.originalUrl}`;

    const headers: Record<string, string> = {};
    if (req.headers['x-user-id']) headers['x-user-id'] = req.headers['x-user-id'] as string;
    if (req.headers['x-user-role']) headers['x-user-role'] = req.headers['x-user-role'] as string;
    if (req.headers['x-user-email']) headers['x-user-email'] = req.headers['x-user-email'] as string;
    if (req.headers['content-type']) headers['content-type'] = req.headers['content-type'] as string;

    const config: AxiosRequestConfig = {
      method: req.method as AxiosRequestConfig['method'],
      url,
      headers,
      data: req.body,
      timeout: 30000,
    };

    try {
      const response = await axios(config);
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response) {
        throw new HttpException(error.response.data, error.response.status);
      }
      throw new HttpException('Service unavailable', 503);
    }
  }
}
