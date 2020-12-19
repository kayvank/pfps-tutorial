package q2io.http.routes.auth

import cats._
import cats.syntax.all._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import q2io.core.algebra.Auth
import q2io.domain.User._
import dev.profunktor.auth.AuthHeaders
import org.http4s.server.AuthMiddleware

final class LogoutRoutes[F[_]: Defer: JsonDecoder: Monad](auth: Auth[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/auth"
  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "logout" as user =>
      AuthHeaders
        .getBearerToken(ar.req)
        .traverse_(t => auth.logout(t, user.value.name)) *> NoContent()
  }
  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] =
    Router(prefixPath -> authMiddleware(httpRoutes))
}
