package q2io.core.algebra

import cats._
import cats.effect._
import cats.effect.implicits._
import cats.syntax.all._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import dev.profunktor.redis4cats.RedisCommands
import scala.concurrent.duration._

import q2io.domain.Healthcheck.AppStatus
import q2io.domain.Healthcheck.PostgresStatus
import q2io.domain.Healthcheck.RedisStatus

trait HealthCheck[F[_]] {
  def status: F[AppStatus]
}

object HealthCheck {
  private val q: Query[Void, Int] =
    sql"""SELECT pid FROM pg_stat_activity""".query(int4)

  def apply[F[_]: Concurrent: Parallel: Timer](
      sessionPool: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[HealthCheck[F]] = Sync[F].delay(
    new HealthCheck[F] {
      override def status: F[AppStatus] = {
        val postgresHealth: F[PostgresStatus] =
          sessionPool
            .use(_.execute(q))
            .map(_.nonEmpty)
            .timeout(1.second)
            .orElse(false.pure[F])
            .map(PostgresStatus.apply(_))
        val redisHealth: F[RedisStatus] =
          redis.ping
            .map(_.nonEmpty)
            .timeout(1.second)
            .orElse(false.pure[F])
            .map(RedisStatus.apply(_))

        (redisHealth, postgresHealth).parMapN(AppStatus)
      }
    }
  )
}
