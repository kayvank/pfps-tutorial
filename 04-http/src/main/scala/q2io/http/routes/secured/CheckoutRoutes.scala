package q2io.http.routes.secured

import cats._
import cats.syntax.all._
import org.http4s._
import org.http4s.server.AuthMiddleware
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import q2io.domain.User._
import q2io.domain.Cart._
import q2io.domain.Order._
import q2io.domain.effects.Effects._
import q2io.domain.Checkout._
import q2io.http.decoder._
import q2io.core.protocol.json._
import q2io.checkout.program.CheckoutProgram

final class CheckoutRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    program: CheckoutProgram[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/checkout"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {

    case ar @ POST -> Root as user =>
      ar.req.decodeR[Card] { card =>
        program
          .checkout(user.value.id, card)
          .flatMap(Created(_))
          .recoverWith {
            case CartNotFound(userId) =>
              NotFound(s"Cart not found for user: ${userId.value}")
            case EmptyCartError =>
              BadRequest("Shopping cart is empty!")
            case PaymentError(cause) =>
              BadRequest(cause)
            case OrderError(cause) =>
              BadRequest(cause)
          }
      }

  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(
      prefixPath -> authMiddleware(httpRoutes)
    )

}
