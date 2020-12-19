package q2io.modules

import cats.effect._
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import retry.RetryPolicy
import retry.RetryPolicies._

import q2io.core.config.Config._
import q2io.domain.effects.Effects._
import q2io.domain.effects.Background
import q2io.checkout.program.CheckoutProgram

final class Programs[F[_]: Background: Logger: MonadThrow: Timer] private (
    checkoutConfig: CheckoutConfig,
    algebras: Algebras[F],
    clients: HttpClients[F]
) {
  val retryPolicy: RetryPolicy[F] =
    limitRetries[F](checkoutConfig.retriesLimit.value) |+| exponentialBackoff(
      checkoutConfig.retriesBackoff
    )

  def checkout: CheckoutProgram[F] = new CheckoutProgram[F](
    clients.payment,
    algebras.cart,
    algebras.orders,
    retryPolicy
  )
}

object Programs {
  def apply[F[_]: Background: Sync: Logger: Timer](
      checkoutConfig: CheckoutConfig,
      algebras: Algebras[F],
      clients: HttpClients[F]
  ): F[Programs[F]] = Sync[F].delay(
    new Programs[F](checkoutConfig, algebras, clients)
  )
}
