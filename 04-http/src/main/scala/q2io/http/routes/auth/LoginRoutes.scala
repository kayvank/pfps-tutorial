package q2io.http.routes.auth

import cats._
import cats.syntax.all._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import q2io.domain.Auth._
import q2io.core.algebra.Auth
import q2io.domain.effects._
import q2io.core.protocol.json._
import q2io.http.decoder._

final class LoginRoutes[F[_]: Defer: JsonDecoder: MonadThrow](auth: Auth[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.decodeR[CreateUser] { user =>
        auth
          .login(user.username.toDomain, user.password.toDomain)
          .flatMap(Ok(_))
          .recoverWith {
            case InvalidUserOrPassword(_) => Forbidden()
          }
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
