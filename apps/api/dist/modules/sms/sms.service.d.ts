import { ConfigService } from '@nestjs/config';
export declare class SmsService {
    private config;
    constructor(config: ConfigService);
    send(phone: string, code: string): Promise<void>;
}
