import { UserService } from './user.service';
import { UpdateProfileDto, UpdatePhoneDto, UpdateAvatarDto } from './dto/user.dto';
export declare class UserController {
    private userService;
    constructor(userService: UserService);
    getProfile(userId: string): Promise<any>;
    updateProfile(userId: string, dto: UpdateProfileDto): Promise<any>;
    updatePhone(userId: string, dto: UpdatePhoneDto): Promise<any>;
    getDevices(userId: string): Promise<any>;
    getStats(userId: string): Promise<{
        totalReadTime: any;
        totalRead: any;
        totalFavorites: any;
    }>;
    updateAvatar(userId: string, dto: UpdateAvatarDto): Promise<any>;
}
