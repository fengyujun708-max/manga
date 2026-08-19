import { NotificationService } from './notification.service';
export declare class NotificationController {
    private notificationService;
    constructor(notificationService: NotificationService);
    getNotifications(userId: string): Promise<any>;
    markRead(userId: string, id: string): Promise<{
        message: string;
    }>;
    getUnreadCount(userId: string): Promise<{
        count: any;
    }>;
}
