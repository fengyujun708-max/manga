import { ExecutionContext, CanActivate } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
export declare const IS_PUBLIC_KEY = "isPublic";
export declare const Public: () => any;
export declare const ROLES_KEY = "roles";
export declare const Roles: (...roles: string[]) => any;
declare const JwtAuthGuard_base: any;
export declare class JwtAuthGuard extends JwtAuthGuard_base {
    private reflector;
    constructor(reflector: Reflector);
    canActivate(context: ExecutionContext): any;
}
export declare class RolesGuard implements CanActivate {
    private reflector;
    constructor(reflector: Reflector);
    canActivate(context: ExecutionContext): boolean;
}
export declare const CurrentUser: any;
export {};
