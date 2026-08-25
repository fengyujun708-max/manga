import { MigrationInterface, QueryRunner } from 'typeorm';

export class CreateSourceRegistry1740000000000 implements MigrationInterface {
  name = 'CreateSourceRegistry1740000000000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE IF NOT EXISTS "source_registry" (
        "uuid" uuid NOT NULL DEFAULT uuid_generate_v4(),
        "id" varchar(128) NOT NULL,
        "name" varchar(256) NOT NULL,
        "description" text,
        "author" varchar(128),
        "repository_url" text,
        "version" varchar(64) NOT NULL,
        "enabled" boolean DEFAULT true,
        "vetted" boolean DEFAULT false,
        "min_app_version" varchar(64),
        "download_url" text,
        "icon_url" text,
        "metadata" jsonb,
        "tags" text,
        "capabilities" text,
        "network_type" varchar(32) DEFAULT 'HTTP',
        "health_status" varchar(32) DEFAULT 'UNKNOWN',
        "health_checked_at" timestamptz,
        "health_error" text,
        "downloads" integer DEFAULT 0,
        "rating" numeric(3,2) DEFAULT 0,
        "created_at" timestamptz DEFAULT now(),
        "updated_at" timestamptz DEFAULT now(),
        CONSTRAINT "PK_source_registry_uuid" PRIMARY KEY ("uuid"),
        CONSTRAINT "UQ_source_registry_id" UNIQUE ("id")
      );
      CREATE INDEX IF NOT EXISTS "IDX_source_registry_enabled_id" ON "source_registry" ("enabled", "id");
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "source_registry" CASCADE;`);
  }
}
