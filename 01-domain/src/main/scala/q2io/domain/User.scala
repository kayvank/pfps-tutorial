package q2io.domain

import java.util.UUID

import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.estatico.newtype.macros.newtype

object User {

  @newtype case class AdminJwtAuth(value: JwtSymmetricAuth)

  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

  @newtype case class UserId(value: UUID)
  @newtype case class UserName(value: String)
  case class User(id: UserId, name: UserName)

  @newtype case class CommonUser(value: User)

  @newtype case class AdminUser(value: User)

}
