package q2io.modules

import cats.effect._
import cats.syntax.all._
import skunk._
import pdi.jwt._
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.RedisCommands
import io.circe.parser.{decode => jsonDecode}

import q2io.core.config.Config.AppConfig
import q2io.core.algebra.Auth
import q2io.core.algebra.AdminAuth
import q2io.core.algebra.Users
import q2io.core.algebra.UserAuth
import q2io.core.algebra.Crypto
import q2io.core.algebra.Tokens
import q2io.domain.effects._
import q2io.domain.Auth._
import q2io.domain.User._

final class Security[F[_]] private (
    val auth: Auth[F],
    val adminAuth: UserAuth[F, AdminUser],
    val usersAuth: UserAuth[F, CommonUser],
    val adminJwtAuth: AdminJwtAuth, //value class
    val userJwtAuth: UserJwtAuth //value class
)
object Security {
  def apply[F[_]: Sync](
      appConfig: AppConfig,
      sessionPool: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[Security[F]] = {

    val adminJwtAuth: AdminJwtAuth = AdminJwtAuth(
      JwtAuth.hmac(
        appConfig.adminJwtConfig.secretKey.value.value.value,
        JwtAlgorithm.HS256
      )
    )
    val userJwtAuth: UserJwtAuth = UserJwtAuth(
      JwtAuth.hmac(
        appConfig.tokenConfig.value.value.value,
        JwtAlgorithm.HS256
      )
    )
    val adminToken = JwtToken(
      appConfig.adminJwtConfig.adminToken.value.value.value
    )

    for {
      adminClaim <- jwtDecode[F](adminToken, adminJwtAuth.value)
      content <- ApThrow[F].fromEither(
        jsonDecode[ClaimContent](adminClaim.content)
      )
      adminUser = AdminUser(User(UserId(content.uuid), UserName("admin")))
      tokens <- Tokens[F](appConfig.tokenConfig, appConfig.tokenExpiration)
      crypto <- Crypto[F](appConfig.passwordSalt)
      users <- Users[F](sessionPool, crypto)
      auth <- Auth[F](appConfig.tokenExpiration, tokens, users, redis)
      adminAuth <- AdminAuth[F](adminToken, adminUser)
      usersAuth <- UserAuth[F](redis)
    } yield (
      new Security[F](auth, adminAuth, usersAuth, adminJwtAuth, userJwtAuth)
    )
  }
}
