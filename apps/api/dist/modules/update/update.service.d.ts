import { Repository } from 'typeorm';
import { AppVersion, RemoteConfig, Announcement } from '../community/entities/community.entity';
export declare class UpdateService {
    private versionRepo;
    private configRepo;
    private announcementRepo;
    constructor(versionRepo: Repository<AppVersion>, configRepo: Repository<RemoteConfig>, announcementRepo: Repository<Announcement>);
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
    getActiveAnnouncements(): Promise<any>;
    private parseVersion;
}
