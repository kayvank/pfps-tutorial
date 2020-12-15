package q2io.core.algebra

import cats._
import cats.syntax.all._
import cats.effect.Sync

import dev.profunktor.auth.jwt.JwtToken
import pdi.jwt.JwtClaim

import q2io.domain.User.{UserName, AdminUser}
import q2io.domain.Auth.Password
import q2io.domain.User.CommonUser
import q2io.domain.effects.GenUUID
import q2io.core.config.Config.TokenExpiration

trait Auth[F[_]] {
  def newUser(userName: UserName, password: Password): F[JwtToken]
  def login(userName: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, userName: UserName): F[Unit]
}
trait UserAuth[F[_], A] {
  def findUser(token: JwtToken)(clain: (JwtClaim)): F[Option[A]]
}

object AdminAuth {
  def apply[F[_]: Sync: Applicative](
      adminToken: JwtToken,
      adminUsedr: AdminUser
  ): UserAuth[F, AdminUser] =
    new UserAuth[F, AdminUser] {
      override def findUser(
          token: JwtToken
      )(clain: (JwtClaim)): F[Option[AdminUser]] =
        ???
    }
}

object UserAuth {
  def apply[F[_]: Sync: Applicative](
      adminToken: JwtToken,
      adminUsedr: CommonUser
  ): UserAuth[F, CommonUser] =
    new UserAuth[F, CommonUser] {
      override def findUser(
          token: JwtToken
      )(clain: (JwtClaim)): F[Option[CommonUser]] =
        ???
    }

  object LiveAuth // {
//     def apply[F[_]: Sync: GenUUID: MonadError](
//       tokenExpiration: TokenExpiration,
//       tokens: Tokens
//         )
//   }
}
