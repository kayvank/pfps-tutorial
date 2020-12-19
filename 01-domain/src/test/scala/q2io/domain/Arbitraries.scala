package q2io.domain

import q2io.domain.Brand._
import q2io.domain.Cart._
import q2io.domain.Category._
import q2io.domain.Checkout._
import q2io.domain.Item._
import q2io.domain.Generators._

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import java.util.UUID
import org.scalacheck.{Arbitrary, Gen}
import squants.market._

object Arbitraries {
  implicit def arbCoercibleInt[A: Coercible[Int, *]]: Arbitrary[A] =
    Arbitrary(cbInt[A])

  implicit def arbCoercibleString[A: Coercible[String, *]]: Arbitrary[A] =
    Arbitrary(cbStr[A])
  implicit def arbCoercibleUUID[A: Coercible[UUID, *]]: Arbitrary[A] =
    Arbitrary(cbUuid[A])

  implicit val arbBrand: Arbitrary[Brand] =
    Arbitrary(brandGen)

  implicit val arbCategory: Arbitrary[Category] =
    Arbitrary(categoryGen)

  implicit val arbMoney: Arbitrary[Money] =
    Arbitrary(genMoney)

  implicit val arbItem: Arbitrary[Item] =
    Arbitrary(itemGen)

  implicit val arbCartItem: Arbitrary[CartItem] =
    Arbitrary(cartItemGen)

  implicit val arbCartTotal: Arbitrary[CartTotal] =
    Arbitrary(cartTotalGen)

  implicit val arbCart: Arbitrary[Cart] =
    Arbitrary(cartGen)

  implicit val arbCard: Arbitrary[Card] =
    Arbitrary(cardGen)
}
