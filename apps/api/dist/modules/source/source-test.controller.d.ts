import { Repository } from 'typeorm';
import { SourceRegistry } from '../community/entities/community.entity';
export declare class SourceTestService {
    private registryRepo;
    constructor(registryRepo: Repository<SourceRegistry>);
    testSource(sourceId: string): Promise<{
        sourceId: string;
        passed: boolean;
        results: {
            name: string;
            passed: boolean;
        }[];
        testedAt: string;
        duration: number;
    }>;
    testAllSources(): Promise<{
        sourceId: string;
        passed: boolean;
        results: {
            name: string;
            passed: boolean;
        }[];
        testedAt: string;
        duration: number;
    }[]>;
    getTestStats(): Promise<{
        total: any;
        passed: any;
        failed: number;
        passRate: string;
    }>;
}
export declare class SourceTestController {
    private sourceTestService;
    constructor(sourceTestService: SourceTestService);
    testSource(sourceId: string): Promise<{
        sourceId: string;
        passed: boolean;
        results: {
            name: string;
            passed: boolean;
        }[];
        testedAt: string;
        duration: number;
    }>;
    testAllSources(): Promise<{
        sourceId: string;
        passed: boolean;
        results: {
            name: string;
            passed: boolean;
        }[];
        testedAt: string;
        duration: number;
    }[]>;
    getStats(): Promise<{
        total: any;
        passed: any;
        failed: number;
        passRate: string;
    }>;
}
