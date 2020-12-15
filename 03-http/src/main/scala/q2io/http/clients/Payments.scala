package q2io.http.clients

import q2io.domain.Payment.Payment
import q2io.domain.Order.PaymentId
import q2io.domain.effects.Effects.BracketThrow
import q2io.core.config.Config.PaymentConfig
import q2io.domain.Order.PaymentError
import q2io.core.protocol.json._

import cats.syntax.all._
import org.http4s.circe.JsonDecoder
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._

trait PaymentClient[F[_]] {
  def process(payment: Payment): F[PaymentId]
}

class LivePaymentClient[F[_]: JsonDecoder: BracketThrow](
    cfg: PaymentConfig,
    client: Client[F]
) extends PaymentClient[F]
    with Http4sClientDsl[F] {
  override def process(payment: Payment): F[PaymentId] =
    Uri
      .fromString(cfg.uri.value.value + "/payments")
      .liftTo[F]
      .flatMap { uri =>
        POST(payment, uri)
          .flatMap(req =>
            client.run(req).use { r =>
              if (r.status == Status.Ok || r.status == Status.Conflict)
                r.asJsonDecode[PaymentId]
              else
                PaymentError(
                  Option(r.status.reason).getOrElse("unknown")
                ).raiseError[F, PaymentId]
            }
          )

      }
}
