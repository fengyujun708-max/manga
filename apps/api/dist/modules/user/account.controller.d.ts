import { AccountService } from './account.service';
export declare class AccountController {
    private accountService;
    constructor(accountService: AccountService);
    deleteAccount(userId: string): Promise<{
        message: string;
    }>;
    getDevices(userId: string): Promise<any>;
    logoutOtherDevices(userId: string): Promise<{
        message: string;
    }>;
}
