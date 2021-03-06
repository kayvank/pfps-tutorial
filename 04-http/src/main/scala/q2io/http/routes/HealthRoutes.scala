package q2io.http.routes

import cats._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import q2io.core.algebra.HealthCheck
import q2io.core.protocol.json._

class HealthRoutes[F[_]: Defer: Monad](healthCheck: HealthCheck[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/healthcheck"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok(healthCheck.status)
  }
  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}
