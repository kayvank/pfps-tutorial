package q2io.domain
package algebra

import cats.effect._

import cats.effect.Sync
import cats.syntax.all._
import dev.profunktor.auth.jwt._
import io.circe.syntax._
import pdi.jwt._

import q2io.domain.effects.GenUUID
import dev.profunktor.auth.jwt.JwtSecretKey
import q2io.core.config.Config._
import scala.concurrent.duration.FiniteDuration
import pdi.jwt.JwtClaim

trait Tokens[F[_]] {
  def create: F[JwtToken]
}

object Token {

  def apply[F[_]: GenUUID: Sync](
      tokenConfig: JwtSecretKeyConfig,
      tokenExpiration: TokenExpiration
  ): F[Tokens[F]] =
    Sync[F].delay(java.time.Clock.systemUTC).map { implicit jClock =>
      new Tokens[F] {
        def create: F[JwtToken] =
          for {
            uuid <- GenUUID[F].make
            claim <- Sync[F].delay(
              JwtClaim(uuid.asJson.noSpaces).issuedNow
                .expiresIn(tokenExpiration.value.toMillis)
            )
            secretKey = JwtSecretKey(tokenConfig.value.value.value)
            token <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
          } yield (token)
      }
    }
}
