import { Repository } from 'typeorm';
import { SourceRegistry } from '../community/entities/community.entity';
export declare class SourceService {
    private registryRepo;
    constructor(registryRepo: Repository<SourceRegistry>);
    getRegistry(): Promise<{
        sources: any;
        updateTime: string;
    }>;
    getSource(id: string): Promise<any>;
    getDownloadUrl(id: string): Promise<{
        url: any;
        sha256: any;
    }>;
    registerSource(dto: any): Promise<any>;
}
