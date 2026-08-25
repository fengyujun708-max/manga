import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { ValidationPipe, Logger } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import * as compression from 'compression';
import helmet from 'helmet';
import { AppModule } from './app.module';

async function bootstrap() {
  const logger = new Logger('Bootstrap');
  const app = await NestFactory.create<NestExpressApplication>(AppModule);

  // Security
  app.use(helmet({
    crossOriginResourcePolicy: { policy: 'cross-origin' },
  }));
  app.enableCors({
    origin: process.env.CORS_ORIGIN?.split(',') || '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
    credentials: true,
  });
  app.use(compression());

  // 静态资源：后台上传内容（/static/uploads/** → UPLOAD_DIR）
  const uploadDir = process.env.UPLOAD_DIR || '/data/uploads';
  app.useStaticAssets(uploadDir, {
    prefix: '/static/uploads/',
    immutable: true,
    maxAge: '30d',
  });

  // Validation
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  // Swagger
  const config = new DocumentBuilder()
    .setTitle('漫界 API')
    .setDescription('漫界漫画阅读平台 API 文档')
    .setVersion('1.0')
    .addBearerAuth()
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api-docs', app, document);

  // Global prefix
  app.setGlobalPrefix('v1');

  const port = process.env.PORT || 3000;
  await app.listen(port);
  logger.log(`漫界 API 已启动: http://localhost:${port}`);
  logger.log(`API 文档: http://localhost:${port}/api-docs`);
}
bootstrap();