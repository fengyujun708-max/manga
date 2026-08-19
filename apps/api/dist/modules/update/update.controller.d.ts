import { UpdateService } from './update.service';
declare class RegisterDeviceDto {
    deviceId: string;
    deviceName?: string;
    pushToken?: string;
}
export declare class UpdateController {
    private updateService;
    constructor(updateService: UpdateService);
    checkUpdate(version: string, platform: string): Promise<{
        hasUpdate: boolean;
        isForceUpdate?: undefined;
        latestVersion?: undefined;
        updateUrl?: undefined;
        changelog?: undefined;
        message?: undefined;
    } | {
        hasUpdate: boolean;
        isForceUpdate: any;
        latestVersion: {
            version: any;
            buildNumber: any;
            platform: any;
        };
        updateUrl: any;
        changelog: any;
        message: string | null;
    }>;
    getRemoteConfig(): Promise<Record<string, any>>;
    getAnnouncements(): Promise<any>;
    registerDevice(userId: string, dto: RegisterDeviceDto): Promise<{
        message: string;
    }>;
}
export {};
