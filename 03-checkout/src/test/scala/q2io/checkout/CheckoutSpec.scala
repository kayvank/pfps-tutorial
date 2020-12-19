package q2io.checkout

import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits.{catsSyntaxEq => _, _}
import retry.RetryPolicy
import retry.RetryPolicies._
import squants.market._

import q2io.suite._
import q2io.domain.Arbitraries._
import q2io.domain.Cart._
import q2io.domain.Item._
import q2io.domain.Checkout._
import q2io.domain.Order._
import q2io.domain.Payment._
import q2io.domain.User._
import q2io.domain.Auth._
import q2io.checkout.clients._
import q2io.core.algebra.Orders
import q2io.core.algebra.ShoppingCart
import q2io.checkout.program.CheckoutProgram

final class CheckoutSpec extends PureTestSuite {

  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  def successfulClient(paymentId: PaymentId): PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(
          payment: q2io.domain.Payment.Payment
      ): IO[q2io.domain.Order.PaymentId] = IO.pure(paymentId)
    }

  def unreachableClient: PaymentClient[IO] = new PaymentClient[IO] {
    def process(
        payment: q2io.domain.Payment.Payment
    ): IO[q2io.domain.Order.PaymentId] =
      IO.raiseError(PaymentError("in-test.unreachable"))
  }

  def recoveringClient(
      attemptsSoFar: Ref[IO, Int],
      paymentId: PaymentId
  ): PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(
          payment: q2io.domain.Payment.Payment
      ): IO[q2io.domain.Order.PaymentId] = attemptsSoFar.get.flatMap {
        case n if n === 1 => IO.pure(paymentId)
        case _ =>
          attemptsSoFar.update(_ + 1) *> IO.raiseError(PaymentError(""))
      }
    }

  def failingOrder: Orders[IO] = new TestOrders {
    override def create(
        userId: UserId,
        paymentId: PaymentId,
        items: List[CartItem],
        total: Money
    ): IO[OrderId] =
      IO.raiseError(OrderError(""))
  }

  def successfulOrders(orderId: OrderId): Orders[IO] = new TestOrders {
    override def create(
        userId: UserId,
        paymentId: PaymentId,
        items: List[CartItem],
        total: Money
    ): IO[OrderId] =
      IO.pure(orderId)
  }

  def emptyCart: ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(CartTotal(List.empty, USD(0)))
  }

  def failingCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit] =
      IO.raiseError(new Exception(""))
  }
  def successfulCart(cartTotal: CartTotal): ShoppingCart[IO] = new TestCart {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)
    override def delete(userId: UserId): IO[Unit] = IO.unit
  }

  test("empty cart") {
    implicit val bg = q2io.checkout.Background.NoOp
    import q2io.domain.Logger.NoOp
    forAll((uid: UserId, pid: PaymentId, oid: OrderId, card: Card) =>
      IOAssertion {
        new CheckoutProgram[IO](
          successfulClient(pid),
          emptyCart,
          successfulOrders(oid),
          retryPolicy
        ).checkout(uid, card)
          .attempt
          .map {
            case Left(EmptyCartError) => assert(true)
            case _                    => fail("Cart was not empty as expected")
          }
      }
    )
  }

  test("unreachable payment client") {
    forAll { (uid: UserId, oid: OrderId, ct: CartTotal, card: Card) =>
      IOAssertion {
        Ref.of[IO, List[String]](List.empty).flatMap { logs =>
          implicit val bg = q2io.checkout.Background.NoOp
          implicit val logger = q2io.domain.Logger.acc(logs)
          new CheckoutProgram[IO](
            unreachableClient,
            successfulCart(ct),
            successfulOrders(oid),
            retryPolicy
          ).checkout(uid, card)
            .attempt
            .flatMap {
              case Left(PaymentError(_)) =>
                logs.get.map {
                  case (x :: xs) =>
                    assert(x.contains("Giving up") && xs.size === MaxRetries)
                  case _ => fail(s"Expected $MaxRetries retries")
                }
              case _ => fail("Expected payment error")
            }
        }
      }
    }
  }

  test("failing payment client succeeds after one retry") {
    forAll {
      (uid: UserId, pid: PaymentId, oid: OrderId, ct: CartTotal, card: Card) =>
        IOAssertion {
          Ref.of[IO, List[String]](List.empty).flatMap { logs =>
            Ref.of[IO, Int](0).flatMap { ref =>
              implicit val bg = q2io.checkout.Background.NoOp
              implicit val logger = q2io.domain.Logger.acc(logs)
              new CheckoutProgram[IO](
                recoveringClient(ref, pid),
                successfulCart(ct),
                successfulOrders(oid),
                retryPolicy
              ).checkout(uid, card)
                .attempt
                .flatMap {
                  case Right(id) =>
                    logs.get.map { xs => assert(id == oid && xs.size == 1) }
                  case Left(_) => fail("Expected Payment Id")
                }
            }
          }
        }
    }
  }
}

protected class TestOrders() extends Orders[IO] {
  def get(userId: UserId, orderId: OrderId): IO[Option[Order]] = ???
  def findBy(userId: UserId): IO[List[Order]] = ???
  def create(
      userId: UserId,
      paymentId: PaymentId,
      items: List[CartItem],
      total: Money
  ): IO[OrderId] = ???
}

protected class TestCart() extends ShoppingCart[IO] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = ???
  def get(userId: UserId): IO[CartTotal] = ???
  def delete(userId: UserId): IO[Unit] = ???
  def removeItem(userId: UserId, itemId: ItemId): IO[Unit] = ???
  def update(userId: UserId, cart: Cart): IO[Unit] = ???
}
