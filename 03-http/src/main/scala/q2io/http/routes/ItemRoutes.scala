package q2io.http.routes

import cats._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import q2io.core.protocol.json._
import q2io.domain.Brand._
import q2io.core.algebra.Items
import q2io.http.Params._

final class ItemRoutes[F[_]: Defer: Monad](items: Items[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/items"

  object BrandQueryParam
      extends OptionalQueryParamDecoderMatcher[BrandParam]("brand")

  private val httpRoutes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root :? BrandQueryParam(brand) =>
        Ok(brand.fold(items.findAll)(b => items.findBy(b.toDomain)))
    }
  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}
