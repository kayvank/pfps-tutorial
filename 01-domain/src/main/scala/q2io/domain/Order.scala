package q2io.domain

import io.estatico.newtype.macros.newtype
import java.util.UUID
import scala.util.control.NoStackTrace
import q2io.domain.Cart._
import q2io.domain.Item._
import squants.market.Money

object Order {
  @newtype case class OrderId(value: UUID)
  @newtype case class PaymentId(value: UUID)

  case class Order(
      id: OrderId,
      paymentId: PaymentId,
      items: Map[ItemId, Quantity],
      total: Money
  )

  case object EmptyCartError extends NoStackTrace
  case class OrderError(cause: String) extends NoStackTrace
  case class PaymentError(cause: String) extends NoStackTrace
}
