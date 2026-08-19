import { Repository } from 'typeorm';
import { Announcement } from '../community/entities/community.entity';
export declare class AnnouncementService {
    private announcementRepo;
    constructor(announcementRepo: Repository<Announcement>);
    getActiveAnnouncements(): Promise<any>;
}
