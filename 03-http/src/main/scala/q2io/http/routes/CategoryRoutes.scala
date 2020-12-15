package q2io.http.routes

import cats._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import q2io.core.protocol.json._
import q2io.core.algebra.Categories

final class CategoryRoutes[F[_]: Defer: Monad](categories: Categories[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/categories"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok(categories.findAll)
  }
  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}
