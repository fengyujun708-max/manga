import { ConfigService } from '@nestjs/config';
import { Repository } from 'typeorm';
import { User } from '../../user/entities/user.entity';
export interface JwtPayload {
    sub: string;
    phone: string;
    role: string;
}
declare const JwtStrategy_base: any;
export declare class JwtStrategy extends JwtStrategy_base {
    private userRepo;
    constructor(config: ConfigService, userRepo: Repository<User>);
    validate(payload: JwtPayload): Promise<User>;
}
declare const JwtRefreshStrategy_base: any;
export declare class JwtRefreshStrategy extends JwtRefreshStrategy_base {
    constructor(config: ConfigService);
    validate(req: any, payload: JwtPayload): Promise<JwtPayload>;
}
export {};
