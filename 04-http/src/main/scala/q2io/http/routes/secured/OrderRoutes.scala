package q2io.http.routes.secured

import cats._
import cats.syntax.all._
import org.http4s._
import org.http4s.server.AuthMiddleware
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import q2io.domain.User._
import q2io.core.algebra.Orders
import q2io.domain.Order._
import q2io.domain.effects.Effects._
import q2io.http.decoder._
import q2io.core.protocol.json._

final class OrderRoutes[F[_]: Defer: JsonDecoder: Monad](
    orders: Orders[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/orders"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {

    case GET -> Root as user =>
      Ok(orders.findBy(user.value.id))

    case GET -> Root / UUIDVar(orderId) as user =>
      Ok(orders.get(user.value.id, OrderId(orderId)))

  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(
      prefixPath -> authMiddleware(httpRoutes)
    )
}
