package q2io.http.routes.admin

import cats._
import io.circe.refined._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server._

import q2io.core.protocol.json._
import q2io.http.decoder._
import q2io.domain.effects.Effects._
import q2io.domain.Brand._
import q2io.core.algebra.Brands
import q2io.domain.User.AdminUser

final class AdminBrandRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    brands: Brands[F]
) extends Http4sDsl[F] {
  private[admin] val prefixPath = "/brands"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[BrandParam] { bp => Created(brands.create(bp.toDomain)) }
  }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] =
    Router(
      prefixPath -> authMiddleware(httpRoutes)
    )
}
