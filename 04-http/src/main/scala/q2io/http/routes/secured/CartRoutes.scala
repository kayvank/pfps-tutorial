package q2io.http.routes.secured

import cats._
import cats.syntax.all._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import q2io.core.algebra.ShoppingCart
import q2io.domain.User._
import q2io.domain.Item._
import q2io.domain.Cart._
import q2io.core.protocol.json._
import org.http4s.server.AuthMiddleware

final class CartRoutes[F[_]: Defer: JsonDecoder: Monad](
    shoppingCart: ShoppingCart[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/cart"
  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {

    // get shopping cart
    case GET -> Root as user => Ok(shoppingCart.get(user.value.id))

    // add items to the cart
    case ar @ POST -> Root as user =>
      ar.req.asJsonDecode[Cart].flatMap { cart =>
        cart.items
          .map {
            case (id, quantity) =>
              shoppingCart.add(user.value.id, id, quantity)
          }
          .toList
          .sequence *> Created()
      }

    // modify items in the cart
    case ar @ PUT -> Root as user =>
      ar.req.asJsonDecode[Cart].flatMap { cart =>
        shoppingCart.update(user.value.id, cart) *> Ok()
      }

    // remove item from users cart
    case DELETE -> Root / UUIDVar(uuid) as user =>
      shoppingCart.removeItem(user.value.id, ItemId(uuid)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
