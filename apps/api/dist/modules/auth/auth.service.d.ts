import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { Repository } from 'typeorm';
import { User } from '../../user/entities/user.entity';
import { UserSession, VerificationCode, LoginSession } from '../../user/entities/user.entity';
export declare class AuthService {
    private userRepo;
    private sessionRepo;
    private verifyCodeRepo;
    private loginSessionRepo;
    private jwtService;
    private config;
    private readonly logger;
    constructor(userRepo: Repository<User>, sessionRepo: Repository<UserSession>, verifyCodeRepo: Repository<VerificationCode>, loginSessionRepo: Repository<LoginSession>, jwtService: JwtService, config: ConfigService);
    sendCode(phone: string): Promise<void>;
    verifyCode(phone: string, code: string, purpose: string): Promise<boolean>;
    register(phone: string, code: string, password: string, nickname: string): Promise<{
        accessToken: any;
        refreshToken: any;
        expiresIn: number;
        user: {
            id: any;
            phone: any;
            nickname: any;
            avatar: any;
            role: any;
        };
    }>;
    login(phone: string, password: string, ip?: string): Promise<{
        accessToken: any;
        refreshToken: any;
        expiresIn: number;
        user: {
            id: any;
            phone: any;
            nickname: any;
            avatar: any;
            role: any;
        };
    }>;
    smsLogin(phone: string, code: string, ip?: string): Promise<{
        accessToken: any;
        refreshToken: any;
        expiresIn: number;
        user: {
            id: any;
            phone: any;
            nickname: any;
            avatar: any;
            role: any;
        };
    }>;
    private generateTokens;
    refreshTokens(refreshToken: string): Promise<{
        accessToken: any;
        refreshToken: any;
        expiresIn: number;
        user: {
            id: any;
            phone: any;
            nickname: any;
            avatar: any;
            role: any;
        };
    }>;
    logout(userId: string, deviceId?: string): Promise<void>;
    logoutAllDevices(userId: string): Promise<void>;
    resetPassword(phone: string, code: string, newPassword: string): Promise<void>;
    changePassword(userId: string, oldPassword: string, newPassword: string): Promise<void>;
    private logLogin;
    cleanExpiredCodes(): Promise<void>;
}
