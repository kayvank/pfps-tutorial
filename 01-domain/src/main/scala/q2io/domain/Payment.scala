package q2io.domain

import User.UserId
import Checkout.Card
import squants.market.Money

object Payment {

  case class Payment(
      id: UserId,
      total: Money,
      card: Card
  )

}
