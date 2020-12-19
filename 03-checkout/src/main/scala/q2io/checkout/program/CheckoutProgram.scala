package q2io.checkout.program

import cats.effect.Timer
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import scala.concurrent.duration._
import retry._
import retry.RetryDetails._
import squants.market.Money

import q2io.checkout.clients.PaymentClient
import q2io.core.algebra._
import q2io.domain.Cart.CartItem
import q2io.domain.Checkout._
import q2io.domain.Order.{PaymentId, _}
import q2io.domain.Payment._
import q2io.domain.User._
import q2io.domain.effects.Background

import q2io.domain.effects.Effects._
import q2io.domain.Cart.CartTotal

final class CheckoutProgram[F[_]: Background: Logger: MonadThrow: Timer](
    paymentClient: PaymentClient[F],
    shoppingCart: ShoppingCart[F],
    orders: Orders[F],
    retryPolicy: RetryPolicy[F]
) {
  private def logError(
      action: String
  )(e: Throwable, details: RetryDetails): F[Unit] = details match {
    case r: WillDelayAndRetry =>
      Logger[F].error(
        s"Failed to process $action with ${e.getMessage}. So far we have retried ${r.retriesSoFar} times."
      )
    case g: GivingUp =>
      Logger[F].error(s"Giving up on $action after ${g.totalRetries} retries.")
  }

  private def processPayment(payment: Payment): F[PaymentId] = {
    val action: F[PaymentId] = retryingOnAllErrors[PaymentId](
      policy = retryPolicy,
      onError = logError("Payments")
    )(paymentClient.process(payment))

    action.adaptError {
      case e => PaymentError(Option(e.getMessage).getOrElse("Unknown"))
    }
  }

  private def createOrder(
      userId: UserId,
      paymentId: PaymentId,
      items: List[CartItem],
      total: Money
  ): F[OrderId] = {
    val action = retryingOnAllErrors[OrderId](
      policy = retryPolicy,
      onError = logError("Order")
    )(orders.create(userId, paymentId, items, total))

    def bgAction(fa: F[OrderId]): F[OrderId] =
      fa.adaptErr {
          case e => OrderError(e.getMessage)
        }
        .onError({
          case _ =>
            Logger[F].error(
              s"Failed to create order for Payment: ${paymentId}. Rescheduling as a background action"
            ) *>
              Background[F].schedule(bgAction(fa), 1.hour)
        })
    bgAction(action)
  }

  def checkout(userId: UserId, card: Card): F[OrderId] =
    shoppingCart
      .get(userId)
      .ensure(EmptyCartError)(_.items.nonEmpty)
      .flatMap {
        case CartTotal(items, total) =>
          for {
            pid <- processPayment((Payment(userId, total, card)))
            o <- createOrder(userId, pid, items, total)
            _ <- shoppingCart.delete(userId).attempt.void
          } yield (o)
      }
}
