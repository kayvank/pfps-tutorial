package q2io.modules

import q2io.core.config.Config.PaymentConfig
import org.http4s.client.Client
import cats.effect.Sync
import q2io.checkout.clients.PaymentClient
import q2io.checkout.clients.LivePaymentClient

trait HttpClients[F[_]] {
  def payment: PaymentClient[F]
}
object HttpClients {
  def apply[F[_]: Sync](
      paymentConfig: PaymentConfig,
      client: Client[F]
  ) = Sync[F].delay(
    new HttpClients[F] {
      override def payment: PaymentClient[F] =
        new LivePaymentClient(paymentConfig, client)
    }
  )
}
