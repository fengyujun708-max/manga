import { DataSource, DataSourceOptions } from 'typeorm';
import { config } from 'dotenv';
config();

const options: DataSourceOptions = {
  type: 'postgres',
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432'),
  username: process.env.DB_USERNAME || 'manjie',
  password: process.env.DB_PASSWORD || 'manjie',
  database: process.env.DB_DATABASE || 'manjie',
  entities: [__dirname + '/../**/*.entity{.ts,.js}'],
  migrations: [__dirname + '/migrations/*{.ts,.js}'],
  synchronize: false,
  logging: process.env.NODE_ENV === 'development',
};

export const AppDataSource = new DataSource(options);