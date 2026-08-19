import { User, UserDevice, UserSession, VerificationCode, LoginSession } from './entities/user.entity';

@Module({
  imports: [TypeOrmModule.forFeature([User, UserDevice, UserSession, VerificationCode, LoginSession])],
  controllers: [UserController, AccountController],
  providers: [UserService, AccountService],
  exports: [TypeOrmModule],
})
export class UserModule {}
