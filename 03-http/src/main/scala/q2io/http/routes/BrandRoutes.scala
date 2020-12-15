package q2io.http
package routes

import cats._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import q2io.core.algebra.Brands
import q2io.core.protocol.json._

final class BrandRoutes[F[_]: Defer: Monad](brands: Brands[F])
    extends Http4sDsl[F] {
  private[routes] val prefixPath = "/brands"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok(brands.findAll) //Requires EntityEncoder[F, A]
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
