export declare enum UserRole {
    SUPER_ADMIN = "super_admin",
    ADMIN = "admin",
    MODERATOR = "moderator",
    SOURCE_MANAGER = "source_manager",
    CONTENT_MANAGER = "content_manager",
    USER = "user",
    BANNED = "banned"
}
export declare enum UserStatus {
    ACTIVE = "active",
    INACTIVE = "inactive",
    SUSPENDED = "suspended",
    DELETED = "deleted"
}
export declare class User {
    id: string;
    phone: string;
    phoneVerified: boolean;
    passwordHash: string;
    nickname: string;
    avatar: string;
    status: UserStatus;
    role: UserRole;
    lastLoginAt: Date;
    lastLoginIp: string;
    createdAt: Date;
    updatedAt: Date;
}
export declare class UserDevice {
    id: string;
    userId: string;
    deviceName: string;
    deviceId: string;
    ip: string;
    lastActiveAt: Date;
    createdAt: Date;
}
export declare class UserSession {
    id: string;
    userId: string;
    refreshToken: string;
    deviceId: string;
    expiresAt: Date;
    isRevoked: boolean;
    createdAt: Date;
    updatedAt: Date;
}
export declare class VerificationCode {
    id: string;
    phone: string;
    code: string;
    purpose: string;
    expiresAt: Date;
    isUsed: boolean;
    attemptCount: number;
    createdAt: Date;
}
export declare class LoginSession {
    id: string;
    userId: string;
    phone: string;
    ip: string;
    userAgent: string;
    success: boolean;
    failReason: string;
    createdAt: Date;
}
