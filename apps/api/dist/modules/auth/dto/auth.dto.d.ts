export declare class SendCodeDto {
    phone: string;
}
export declare class VerifyCodeDto {
    phone: string;
    code: string;
}
export declare class RegisterDto {
    phone: string;
    code: string;
    password: string;
    nickname: string;
}
export declare class LoginDto {
    phone: string;
    password: string;
}
export declare class SmsLoginDto {
    phone: string;
    code: string;
}
export declare class RefreshTokenDto {
    refreshToken: string;
}
export declare class ResetPasswordDto {
    phone: string;
    code: string;
    newPassword: string;
}
export declare class ChangePasswordDto {
    oldPassword: string;
    newPassword: string;
}
export declare class TokenResponse {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
}
