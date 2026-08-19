import { Repository } from 'typeorm';
import { User } from '../user/entities/user.entity';
import { Report, Ban, AuditLog } from '../community/entities/community.entity';
export declare class AdminService {
    private userRepo;
    private reportRepo;
    private banRepo;
    private auditRepo;
    constructor(userRepo: Repository<User>, reportRepo: Repository<Report>, banRepo: Repository<Ban>, auditRepo: Repository<AuditLog>);
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
