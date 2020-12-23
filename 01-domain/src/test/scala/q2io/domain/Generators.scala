package q2io.domain

import q2io.domain.Brand._
import q2io.domain.Cart._
import q2io.domain.Category._
import q2io.domain.Checkout._
import q2io.domain.Item._

import squants.market._
import eu.timepit.refined.api.Refined
import io.estatico.newtype.ops._
import io.estatico.newtype.Coercible
import org.scalacheck.Gen
import java.util.UUID

object Generators {

  def cbUuid[A: Coercible[UUID, *]]: Gen[A] =
    Gen.uuid.map(_.coerce[A])

  def cbStr[A: Coercible[String, *]]: Gen[A] =
    genNonEmptyString.map(_.coerce[A])

  def cbInt[A: Coercible[Int, *]]: Gen[A] =
    Gen.posNum[Int].map(_.coerce[A])

  val genMoney: Gen[Money] = Gen.posNum[Long].map(n => USD(BigDecimal(n)))

  val genNonEmptyString: Gen[String] = Gen
    .chooseNum(21, 40)
    .flatMap(n => Gen.buildableOfN[String, Char](n, Gen.alphaChar))

  val brandGen: Gen[Brand] = for {
    i <- cbUuid[BrandId]
    n <- cbStr[BrandName]
  } yield (Brand(uuid = i, name = n))

  val categoryGen: Gen[Category] = for {
    i <- cbUuid[CategoryId]
    n <- cbStr[CategoryName]
  } yield Category(uuid = i, name = n)

  val itemGen: Gen[Item] = for {
    n <- cbStr[ItemName]
    d <- cbStr[ItemDescription]
    i <- cbUuid[ItemId]
    p <- genMoney
    b <- brandGen
    c <- categoryGen
  } yield (Item(
    uuid = i,
    name = n,
    description = d,
    price = p,
    brand = b,
    category = c
  ))

  val cartItemGen: Gen[CartItem] = for {
    i <- itemGen
    q <- cbInt[Quantity]
  } yield (CartItem(item = i, quantity = q))

  val cartTotalGen: Gen[CartTotal] =
    for {
      i <- Gen.nonEmptyListOf(cartItemGen)
      t <- genMoney
    } yield CartTotal(items = i, total = t)

  val itemMapGen: Gen[(ItemId, Quantity)] = for {
    i <- cbUuid[ItemId]
    q <- cbInt[Quantity]
  } yield (i -> q)

  val cartGen: Gen[Cart] = Gen.nonEmptyMap(itemMapGen).map(Cart.apply)

  val cardGen: Gen[Card] = for {
    n <- genNonEmptyString.map[CardNamePred](Refined.unsafeApply)
    u <- Gen.posNum[Long].map[CardNumberPred](Refined.unsafeApply)
    x <- Gen
      .posNum[Int]
      .map[CardExpirationPred](x => Refined.unsafeApply(x.toString))
    c <- Gen.posNum[Int].map[CardCVVPred](Refined.unsafeApply)
  } yield Card(CardName(n), CardNumber(u), CardExpiration(x), CardCVV(c))
}
