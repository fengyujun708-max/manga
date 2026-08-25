import { MigrationInterface, QueryRunner } from "typeorm";

export class CreateClientLogsTable1730000000001 implements MigrationInterface {
    name = 'CreateClientLogsTable1730000000001';

    public async up(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`
            CREATE TABLE IF NOT EXISTS "client_logs" (
                "id" uuid NOT NULL DEFAULT uuid_generate_v4(),
                "userId" character varying(36),
                "level" character varying(30) NOT NULL,
                "title" character varying(200) NOT NULL,
                "content" text,
                "deviceModel" character varying(50),
                "appVersion" character varying(30),
                "platform" character varying(30),
                "createdAt" TIMESTAMP NOT NULL DEFAULT now(),
                CONSTRAINT "PK_client_logs_id" PRIMARY KEY ("id")
            );
            CREATE INDEX IF NOT EXISTS "IDX_client_logs_createdAt" ON "client_logs" ("createdAt");
        `);
    }

    public async down(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`DROP TABLE IF EXISTS "client_logs"`);
    }
}
