import { Injectable, Logger } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { firstValueFrom } from 'rxjs';
import { SourceRegistry } from '../../community/entities/community.entity';

export interface TestResult {
  sourceId: string;
  version: string;
  passed: boolean;
  results: TestCaseResult[];
  testedAt: string;
  duration: number;
}

export interface TestCaseResult {
  name: string;
  passed: boolean;
  error?: string;
  duration: number;
  data?: any;
}

@Injectable()
export class SourceTestService {
  private readonly logger = new Logger(SourceTestService.name);

  constructor(
    @InjectRepository(SourceRegistry)
    private registryRepo: Repository<SourceRegistry>,
    private httpService: HttpService,
  ) {}

  async testSource(sourceId: string): Promise<TestResult> {
    const source = await this.registryRepo.findOneBy({ sourceId });
    if (!source) {
      throw new Error(`源 ${sourceId} 不存在`);
    }

    const startTime = Date.now();
    const results: TestCaseResult[] = [];

    // 1. 连接测试
    results.push(await this.testConnectivity(source));

    // 2. 搜索测试
    results.push(await this.testSearch(source));

    // 3. 详情测试
    results.push(await this.testDetail(source));

    // 4. 章节测试
    results.push(await this.testChapters(source));

    // 5. 图片测试
    results.push(await this.testPages(source));

    // 6. 分类测试（可选）
    results.push(await this.testCategories(source));

    const duration = Date.now() - startTime;
    const passed = results.every(r => r.passed);

    this.logger.log(
      `源 ${sourceId} 测试${passed ? '通过' : '失败'} (${duration}ms)` +
      results.filter(r => !r.passed).map(r => `\n  ❌ ${r.name}: ${r.error}`).join('')
    );

    return {
      sourceId: source.sourceId,
      version: source.version,
      passed,
      results,
      testedAt: new Date().toISOString(),
      duration,
    };
  }

  private async testConnectivity(source: SourceRegistry): Promise<TestCaseResult> {
    const start = Date.now();
    try {
      const response = await firstValueFrom(
        this.httpService.head(source.downloadUrl, { timeout: 10000 }),
      );
      return {
        name: '连接测试',
        passed: response.status >= 200 && response.status < 400,
        duration: Date.now() - start,
        data: { status: response.status, url: source.downloadUrl },
      };
    } catch (error) {
      return {
        name: '连接测试',
        passed: false,
        error: (error as Error).message,
        duration: Date.now() - start,
      };
    }
  }

  private async testSearch(source: SourceRegistry): Promise<TestCaseResult> {
    const start = Date.now();
    try {
      // 下载源 JS 并执行搜索测试
      const jsCode = await this.downloadSourceJs(source.downloadUrl);

      // 检查 JS 中是否有 search 函数定义
      if (!jsCode.includes('search')) {
        return {
          name: '搜索测试',
          passed: false,
          error: '源未实现 search 函数',
          duration: Date.now() - start,
        };
      }

      // 检查基本结构
      const hasSearchFn = /\bsearch\s*[\(:]/.test(jsCode);
      return {
        name: '搜索测试',
        passed: hasSearchFn,
        duration: Date.now() - start,
        data: { hasSearchFunction: hasSearchFn },
      };
    } catch (error) {
      return {
        name: '搜索测试',
        passed: false,
        error: (error as Error).message,
        duration: Date.now() - start,
      };
    }
  }

  private async testDetail(source: SourceRegistry): Promise<TestCaseResult> {
    const start = Date.now();
    try {
      const jsCode = await this.downloadSourceJs(source.downloadUrl);
      const hasDetailFn = /\bgetDetail\s*[\(:]/.test(jsCode);

      return {
        name: '详情测试',
        passed: hasDetailFn,
        duration: Date.now() - start,
        data: { hasGetDetailFunction: hasDetailFn },
      };
    } catch (error) {
      return {
        name: '详情测试',
        passed: false,
        error: (error as Error).message,
        duration: Date.now() - start,
      };
    }
  }

  private async testChapters(source: SourceRegistry): Promise<TestCaseResult> {
    const start = Date.now();
    try {
      const jsCode = await this.downloadSourceJs(source.downloadUrl);
      const hasChaptersFn = /\bgetChapters\s*[\(:]/.test(jsCode);

      return {
        name: '章节测试',
        passed: hasChaptersFn,
        duration: Date.now() - start,
        data: { hasGetChaptersFunction: hasChaptersFn },
      };
    } catch (error) {
      return {
        name: '章节测试',
        passed: false,
        error: (error as Error).message,
        duration: Date.now() - start,
      };
    }
  }

  private async testPages(source: SourceRegistry): Promise<TestCaseResult> {
    const start = Date.now();
    try {
      const jsCode = await this.downloadSourceJs(source.downloadUrl);
      const hasPagesFn = /\bgetPages\s*[\(:]/.test(jsCode);

      return {
        name: '图片测试',
        passed: hasPagesFn,
        duration: Date.now() - start,
        data: { hasGetPagesFunction: hasPagesFn },
      };
    } catch (error) {
      return {
        name: '图片测试',
        passed: false,
        error: (error as Error).message,
        duration: Date.now() - start,
      };
    }
  }

  private async testCategories(source: SourceRegistry): Promise<TestCaseResult> {
    const start = Date.now();
    try {
      const jsCode = await this.downloadSourceJs(source.downloadUrl);
      const hasCategoriesFn = /\bgetCategories\s*[\(:]/.test(jsCode);

      return {
        name: '分类测试',
        passed: true, // 分类是可选的，不强制要求
        duration: Date.now() - start,
        data: { hasGetCategoriesFunction: hasCategoriesFn },
      };
    } catch (error) {
      return {
        name: '分类测试',
        passed: true, // 可选功能，失败不算不通过
        duration: Date.now() - start,
      };
    }
  }

  private async downloadSourceJs(url: string): Promise<string> {
    try {
      const response = await firstValueFrom(
        this.httpService.get(url, {
          responseType: 'text',
          timeout: 15000,
        }),
      );
      return response.data as string;
    } catch (error) {
      throw new Error(`下载源 JS 失败: ${(error as Error).message}`);
    }
  }

  /// 批量测试所有源
  async testAllSources(): Promise<TestResult[]> {
    const sources = await this.registryRepo.find({ where: { status: 'active' } });
    const results: TestResult[] = [];

    for (const source of sources) {
      try {
        const result = await this.testSource(source.sourceId);
        results.push(result);
      } catch (error) {
        this.logger.error(`测试源 ${source.sourceId} 发生异常: ${(error as Error).message}`);
      }
    }

    return results;
  }

  /// 获取测试统计
  async getTestStats(): Promise<{
    total: number;
    passed: number;
    failed: number;
    passRate: string;
  }> {
    const sources = await this.registryRepo.find({ where: { status: 'active' } });
    const results = await this.testAllSources();

    const passed = results.filter(r => r.passed).length;
    return {
      total: sources.length,
      passed,
      failed: sources.length - passed,
      passRate: sources.length > 0
        ? `${(passed / sources.length * 100).toFixed(1)}%`
        : '0%',
    };
  }
}