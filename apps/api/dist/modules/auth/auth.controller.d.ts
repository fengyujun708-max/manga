import { AuthService } from './auth.service';
import { SendCodeDto, RegisterDto, LoginDto, SmsLoginDto, RefreshTokenDto, ResetPasswordDto, ChangePasswordDto } from './dto/auth.dto';
export declare class AuthController {
    private authService;
    constructor(authService: AuthService);
    sendCode(dto: SendCodeDto): Promise<{
        message: string;
    }>;
    register(dto: RegisterDto, ip: string): Promise<{
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
    login(dto: LoginDto, ip: string): Promise<{
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
    smsLogin(dto: SmsLoginDto, ip: string): Promise<{
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
    refresh(dto: RefreshTokenDto): Promise<{
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
    logout(userId: string): Promise<{
        message: string;
    }>;
    logoutAll(userId: string): Promise<{
        message: string;
    }>;
    resetPassword(dto: ResetPasswordDto): Promise<{
        message: string;
    }>;
    changePassword(userId: string, dto: ChangePasswordDto): Promise<{
        message: string;
    }>;
}
