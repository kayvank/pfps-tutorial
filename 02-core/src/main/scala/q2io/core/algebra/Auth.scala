package q2io.core.algebra

import cats._
import cats.syntax.all._
import cats.effect.Sync
import io.circe.syntax._
import io.circe.parser.decode
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import pdi.jwt.JwtClaim

import q2io.domain.User._
import q2io.domain.Auth._
import q2io.domain.effects.GenUUID
import q2io.core.config.Config.TokenExpiration
import q2io.core.algebra.Tokens
import q2io.domain.effects.Effects.MonadThrow
import q2io.core.protocol.json._

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
      adminUser: AdminUser
  ): F[UserAuth[F, AdminUser]] =
    Sync[F].delay(new UserAuth[F, AdminUser] {
      override def findUser(
          token: JwtToken
      )(clain: (JwtClaim)): F[Option[AdminUser]] =
        (token == adminToken).guard[Option].as(adminUser).pure[F]
    })
}

object UserAuth {
  def apply[F[_]: Sync: Functor](
      redis: RedisCommands[F, String, String]
  ): F[UserAuth[F, CommonUser]] =
    Sync[F].delay(
      new UserAuth[F, CommonUser] {
        override def findUser(
            token: JwtToken
        )(claim: (JwtClaim)): F[Option[CommonUser]] =
          redis
            .get(token.value)
            .map(
              _.flatMap { u =>
                decode[User](u).toOption.map(CommonUser.apply(_))
              }
            )
      }
    )
}
object Auth {

  def apply[F[_]: Sync: GenUUID: MonadThrow](
      tokenExpiration: TokenExpiration,
      tokens: Tokens[F],
      users: Users[F],
      redis: RedisCommands[F, String, String]
  ): F[Auth[F]] =
    Sync[F].delay(new Auth[F] {

      /**
        * Desc: Creating a user means:
        * persisting it in Postgres,
        * creating a JWT token,
        * serializing the user as JSON,
        * and persisting both the token and the serialized user in Redis for fast access,
        * indicating an expiration time
        * */
      override def newUser(
          username: UserName,
          password: Password
      ): F[JwtToken] =
        users.find(username, password).flatMap {
          case Some(_) =>
            UserNameInUse(username).raiseError[F, JwtToken]
          case None =>
            for {
              i <- users.create(username, password)
              t <- tokens.create
              u = User(i, username).asJson.noSpaces
              _ <- redis.setEx(t.value, u, tokenExpiration.value)
              _ <- redis.setEx(username.value, t.value, tokenExpiration.value)
            } yield (t)
        }

      override def login(username: UserName, password: Password): F[JwtToken] =
        users.find(username, password).flatMap {
          case None =>
            InvalidUserOrPassword(username).raiseError[F, JwtToken]
          case Some(user) =>
            redis.get(username.value).flatMap {
              case Some(t) =>
                JwtToken(t).pure[F]
              case None =>
                tokens.create.flatTap { t => // Tap <=> peek
                  redis.setEx(
                    t.value,
                    user.asJson.noSpaces,
                    tokenExpiration.value
                  ) *> redis
                    .setEx(username.value, t.value, tokenExpiration.value)
                }
            }
        }

      override def logout(token: JwtToken, userName: UserName): F[Unit] =
        redis.del(token.value) *> redis.del(userName.value)

    })
}
