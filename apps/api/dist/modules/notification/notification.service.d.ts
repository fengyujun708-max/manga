import { Repository } from 'typeorm';
import { Notification } from '../community/entities/community.entity';
export declare class NotificationService {
    private notifRepo;
    constructor(notifRepo: Repository<Notification>);
    getNotifications(userId: string): Promise<any>;
    markRead(userId: string, id: string): Promise<{
        message: string;
    }>;
    getUnreadCount(userId: string): Promise<{
        count: any;
    }>;
}
