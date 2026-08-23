import { MigrationInterface, QueryRunner, Table, TableIndex } from 'typeorm';

export class CreateOfficialTables1730000000000 implements MigrationInterface {
  name = 'CreateOfficialTables1730000000000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    // official_channels
    await queryRunner.createTable(new Table({
      name: 'official_channels',
      columns: [
        { name: 'id', type: 'uuid', isPrimary: true, generationStrategy: 'uuid', default: 'gen_random_uuid()' },
        { name: 'slug', type: 'varchar', length: '64', isUnique: true },
        { name: 'display_name', type: 'varchar', length: '128' },
        { name: 'description', type: 'text', isNullable: true },
        { name: 'logo_url', type: 'text', isNullable: true },
        { name: 'provider', type: 'varchar', length: '64' },
        { name: 'status', type: 'varchar', length: '16', default: "'ACTIVE'" },
        { name: 'priority', type: 'int', default: 0 },
        { name: 'created_at', type: 'timestamptz', default: 'now()' },
        { name: 'updated_at', type: 'timestamptz', default: 'now()' },
      ],
    }));

    // official_series
    await queryRunner.createTable(new Table({
      name: 'official_series',
      columns: [
        { name: 'id', type: 'uuid', isPrimary: true, generationStrategy: 'uuid', default: 'gen_random_uuid()' },
        { name: 'channel_id', type: 'uuid' },
        { name: 'provider_id', type: 'varchar', length: '128' },
        { name: 'title', type: 'varchar', length: '512' },
        { name: 'alt_title', type: 'varchar', length: '512', isNullable: true },
        { name: 'author', type: 'varchar', length: '256', isNullable: true },
        { name: 'artist', type: 'varchar', length: '256', isNullable: true },
        { name: 'description', type: 'text', isNullable: true },
        { name: 'cover_url', type: 'text', isNullable: true },
        { name: 'genres', type: 'simple-array', isNullable: true },
        { name: 'status', type: 'varchar', length: '16', default: "'ONGOING'" },
        { name: 'provider_url', type: 'text', isNullable: true },
        { name: 'language', type: 'varchar', length: '8', default: "'zh'" },
        { name: 'mature', type: 'boolean', default: false },
        { name: 'created_at', type: 'timestamptz', default: 'now()' },
        { name: 'updated_at', type: 'timestamptz', default: 'now()' },
      ],
      foreignKeys: [
        {
          columnNames: ['channel_id'],
          referencedTableName: 'official_channels',
          referencedColumnNames: ['id'],
          onDelete: 'CASCADE',
        },
      ],
    }));

    await queryRunner.createIndex('official_series', new TableIndex({
      name: 'IDX_OFFICIAL_SERIES_CHANNEL',
      columnNames: ['channel_id', 'provider_id'],
      isUnique: true,
    }));

    // official_episodes
    await queryRunner.createTable(new Table({
      name: 'official_episodes',
      columns: [
        { name: 'id', type: 'uuid', isPrimary: true, generationStrategy: 'uuid', default: 'gen_random_uuid()' },
        { name: 'series_id', type: 'uuid' },
        { name: 'ep_number', type: 'int' },
        { name: 'title', type: 'varchar', length: '512', isNullable: true },
        { name: 'sub_title', type: 'varchar', length: '512', isNullable: true },
        { name: 'published_at', type: 'timestamptz', isNullable: true },
        { name: 'availability', type: 'varchar', length: '16', default: "'AVAILABLE'" },
        { name: 'external_url', type: 'text', isNullable: true },
        { name: 'asset_url', type: 'text', isNullable: true },
        { name: 'asset_type', type: 'varchar', length: '16', default: "'image'" },
        { name: 'metadata', type: 'jsonb', default: "'{}'::jsonb" },
        { name: 'created_at', type: 'timestamptz', default: 'now()' },
        { name: 'updated_at', type: 'timestamptz', default: 'now()' },
      ],
      foreignKeys: [
        {
          columnNames: ['series_id'],
          referencedTableName: 'official_series',
          referencedColumnNames: ['id'],
          onDelete: 'CASCADE',
        },
      ],
    }));

    await queryRunner.createIndex('official_episodes', new TableIndex({
      name: 'IDX_OFFICIAL_EPISODES_SERIES',
      columnNames: ['series_id', 'ep_number'],
      isUnique: true,
    }));
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.dropTable('official_episodes');
    await queryRunner.dropTable('official_series');
    await queryRunner.dropTable('official_channels');
  }
}