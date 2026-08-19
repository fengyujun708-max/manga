import { SourceService } from './source.service';
export declare class SourceController {
    private sourceService;
    constructor(sourceService: SourceService);
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
