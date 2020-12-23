package q2io.http.routes.admin

import cats._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server._

import q2io.core.protocol.json._
import q2io.http.decoder._
import q2io.domain.effects._
import q2io.domain.User.AdminUser
import q2io.core.algebra.Items
import q2io.domain.Item.CreateItemParam
import q2io.domain.Item.UpdateItemParam

final class AdminItemRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    items: Items[F]
) extends Http4sDsl[F] {
  private[admin] val prefixPath = "/items"

  private val httpRoutes: AuthedRoutes[AdminUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root as _ =>
      ar.req.decodeR[CreateItemParam] { ip =>
        Created(items.create(ip.toDomain))
      }
    // update price of an item
    case ar @ PUT -> Root as _ =>
      ar.req.decodeR[UpdateItemParam] { up => Ok(items.update(up.toDomain)) }
  }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] =
    Router(
      prefixPath -> authMiddleware(httpRoutes)
    )
}
