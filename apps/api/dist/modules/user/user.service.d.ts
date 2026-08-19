import { Repository } from 'typeorm';
import { User } from './entities/user.entity';
import { UserDevice } from './entities/user.entity';
import { ReadingHistory, Favorite } from '../comic/entities/comic.entity';
export declare class UserService {
    private userRepo;
    private deviceRepo;
    private historyRepo;
    private favoriteRepo;
    constructor(userRepo: Repository<User>, deviceRepo: Repository<UserDevice>, historyRepo: Repository<ReadingHistory>, favoriteRepo: Repository<Favorite>);
    getProfile(userId: string): Promise<any>;
    updateProfile(userId: string, dto: {
        nickname: string;
    }): Promise<any>;
    updatePhone(userId: string, phone: string, code: string): Promise<any>;
    updateAvatar(userId: string, url: string): Promise<any>;
    getDevices(userId: string): Promise<any>;
    getStats(userId: string): Promise<{
        totalReadTime: any;
        totalRead: any;
        totalFavorites: any;
    }>;
}
