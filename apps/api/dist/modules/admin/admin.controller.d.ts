import { AdminService } from './admin.service';
export declare class AdminController {
    private adminService;
    constructor(adminService: AdminService);
    getDashboard(): Promise<{
        totalUsers: any;
        activeUsers: any;
        pendingReports: any;
    }>;
    getUsers(page?: number, limit?: number): Promise<{
        items: any;
        total: any;
        page: number;
        limit: number;
        totalPages: number;
    }>;
    banUser(adminId: string, userId: string, reason: string): Promise<{
        message: string;
    }>;
    getReports(): Promise<any>;
    resolveReport(id: string): Promise<{
        message: string;
    }>;
}
