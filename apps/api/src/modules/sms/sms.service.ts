import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class SmsService {
  constructor(private config: ConfigService) {}

  async send(phone: string, code: string): Promise<void> {
    console.log(`[SMS-DEV] 验证码 ${code} -> ${phone}`);
  }
}