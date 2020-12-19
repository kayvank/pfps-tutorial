package q2io.bootstrap

import cats.effect._
import cats.syntax.all._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.log4cats._
import eu.timepit.refined.auto._
import io.chrisdavenport.log4cats.Logger
import natchez.Trace.Implicits.noop // needed for skunk
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import scala.concurrent.ExecutionContext
import skunk._

import q2io.core.config.Config._

final case class AppResources[F[_]](
    client: Client[F],
    psql: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
)
object AppResources {

  private def mkPostgresSqlResource[F[_]: ConcurrentEffect: ContextShift](
      c: PostgreSQLConfig
  ): SessionPool[F] =
    Session.pooled[F](
      host = c.host,
      port = c.port.value,
      user = c.user.value,
      database = c.database.value,
      max = c.max.value
    )

  private def mkRedisResource[F[_]: ConcurrentEffect: ContextShift: Logger](
      c: RedisConfig
  ): Resource[F, RedisCommands[F, String, String]] = Redis[F].utf8(c.uri.value)

  private def mkHttpClient[F[_]: ConcurrentEffect: ContextShift](
      c: HttpClientConfig
  ): Resource[F, Client[F]] =
    BlazeClientBuilder[F](ExecutionContext.global)
      .withConnectTimeout(c.connectTimeout)
      .withRequestTimeout(c.requestTimeout)
      .resource

  def apply[F[_]: ConcurrentEffect: ContextShift: Logger](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] =
    (
      mkHttpClient(cfg.httpClientConfig),
      mkPostgresSqlResource(cfg.postgreSQL),
      mkRedisResource(cfg.redis)
    ).mapN(AppResources.apply[F])
}
