import { AnnouncementService } from './announcement.service';
export declare class AnnouncementController {
    private announcementService;
    constructor(announcementService: AnnouncementService);
    getActiveAnnouncements(): Promise<any>;
}
