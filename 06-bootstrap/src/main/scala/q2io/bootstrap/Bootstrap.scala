package q2io.bootstrap

import cats.effect._
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext
import q2io.core.config.Load

import q2io.modules._

object Bootstrap extends IOApp {
  implicit val logger = Slf4jLogger.getLogger[IO]
  override def run(args: List[String]): IO[ExitCode] =
    Load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        AppResources[IO](cfg).use { res =>
          for {
            security <- Security[IO](cfg, res.psql, res.redis)
            algebras <- Algebras[IO](res.redis, res.psql, cfg.cartExpiration)
            clients <- HttpClients[IO](cfg.paymentConfig, res.client)
            programs <- Programs[IO](cfg.checkoutConfig, algebras, clients)
            api <- HttpApi[IO](algebras, programs, security)
            _ <- BlazeServerBuilder[IO](ExecutionContext.global)
              .bindHttp(
                cfg.httpServerConfig.port.value,
                cfg.httpServerConfig.host.value
              )
              .withHttpApp(api.httpApp)
              .serve
              .compile
              .drain
          } yield ExitCode.Success

        }
    }

}
