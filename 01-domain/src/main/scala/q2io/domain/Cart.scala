package q2io.domain

import io.estatico.newtype.macros.newtype
import Item._
import java.util.UUID
import scala.util.control.NoStackTrace
import User.UserId
import squants.market.Money

object Cart {
  @newtype case class Quantity(value: Int)
  @newtype case class Cart(items: Map[ItemId, Quantity])
  @newtype case class CartId(value: UUID)

  case class CartItem(item: Item, quantity: Quantity)
  case class CartTotal(items: List[CartItem], total: Money)

  case class CartNotFound(userId: UserId) extends NoStackTrace
}
