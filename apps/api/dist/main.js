"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const core_1 = require("@nestjs/core");
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const compression = require("compression");
const helmet_1 = require("helmet");
const app_module_1 = require("./app.module");
async function bootstrap() {
    const logger = new common_1.Logger('Bootstrap');
    const app = await core_1.NestFactory.create(app_module_1.AppModule);
    app.use((0, helmet_1.default)());
    app.enableCors({
        origin: process.env.CORS_ORIGIN?.split(',') || '*',
        methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
        credentials: true,
    });
    app.use(compression());
    app.useGlobalPipes(new common_1.ValidationPipe({
        whitelist: true,
        forbidNonWhitelisted: true,
        transform: true,
    }));
    const config = new swagger_1.DocumentBuilder()
        .setTitle('漫界 API')
        .setDescription('漫界漫画阅读平台 API 文档')
        .setVersion('1.0')
        .addBearerAuth()
        .build();
    const document = swagger_1.SwaggerModule.createDocument(app, config);
    swagger_1.SwaggerModule.setup('api-docs', app, document);
    app.setGlobalPrefix('v1');
    const port = process.env.PORT || 3000;
    await app.listen(port);
    logger.log(`漫界 API 已启动: http://localhost:${port}`);
    logger.log(`API 文档: http://localhost:${port}/api-docs`);
}
bootstrap();
//# sourceMappingURL=main.js.map