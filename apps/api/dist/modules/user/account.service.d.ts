import { Repository } from 'typeorm';
import { User } from './entities/user.entity';
import { UserSession, UserDevice } from './entities/user.entity';
export declare class AccountService {
    private userRepo;
    private sessionRepo;
    private deviceRepo;
    constructor(userRepo: Repository<User>, sessionRepo: Repository<UserSession>, deviceRepo: Repository<UserDevice>);
    deleteAccount(userId: string): Promise<{
        message: string;
    }>;
    getDevices(userId: string): Promise<any>;
    logoutOtherDevices(userId: string): Promise<{
        message: string;
    }>;
}
