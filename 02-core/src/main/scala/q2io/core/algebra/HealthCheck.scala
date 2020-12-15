package q2io.core.algebra

import cats._
import cats.effect._
import cats.effect.implicits._
import cats.syntax.all._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import dev.profunktor.redis4cats.RedisCommands

import q2io.domain.Healthcheck.AppStatus

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}

object HealthChack {
  def apply[F[_]: Concurrent: Parallel: Timer](
      sessionPool: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[HealthCheck[F]] = Sync[F].delay(
    new HealthCheck[F] {
      override def status: F[AppStatus] = ???
    }
  )
}
