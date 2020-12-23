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
import q2io.domain.Background

final class CheckoutSpec extends PureTestSuite {

  val MaxRetries = 3

  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  def successfulClient(paymentId: PaymentId): PaymentClient[IO] =
    new PaymentClient[IO] {
      def process(
          payment: q2io.domain.Payment.Payment
      ): IO[PaymentId] = IO.pure(paymentId)
    }

  def unreachableClient: PaymentClient[IO] = new PaymentClient[IO] {
    def process(
        payment: q2io.domain.Payment.Payment
    ): IO[PaymentId] =
      IO.raiseError(PaymentError(""))
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

  def failingOrders: Orders[IO] = new TestOrders {
    override def create(
        userId: UserId,
        paymentId: PaymentId,
        items: List[CartItem],
        total: Money
    ): IO[OrderId] = IO.raiseError(OrderError(""))
  }

  test("empty cart") {
    implicit val bg = Background.NoOp
    import q2io.domain.Logger.NoOp
    forAll(
      (
          uid: UserId,
          pid: PaymentId,
          oid: OrderId,
          card: Card,
          ct: CartTotal
      ) =>
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

  test("itemGEN gen") {
    forAll { (item: Item) => assert(item.name.value.length >= 1) }
  }

  test("orderid gen") {
    forAll { (uid: UserId, oid: OrderId) =>
      assert(!uid.value.toString.isEmpty)
    }
  }

  test("unreachable payment client") {
    forAll { (uid: UserId, oid: OrderId, ct: CartTotal, card: Card) =>
      IOAssertion {
        Ref.of[IO, List[String]](List.empty).flatMap { logs =>
          implicit val bg = Background.NoOp
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
              implicit val bg = Background.NoOp
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

  test("cannot create order, run in the background") {
    import Background._
    forAll { (uid: UserId, pid: PaymentId, ct: CartTotal, card: Card) =>
      IOAssertion {
        Ref.of[IO, Int](0).flatMap { ref =>
          Ref.of[IO, List[String]](List.empty).flatMap { logs =>
            implicit val bg = Background.counter(ref)
            implicit val logger = q2io.domain.Logger.acc(logs)
            new CheckoutProgram[IO](
              successfulClient(pid),
              successfulCart(ct),
              failingOrders,
              retryPolicy
            ).checkout(uid, card)
              .attempt
              .flatMap {
                case Left(OrderError(_)) =>
                  (ref.get, logs.get).mapN {
                    case (c, (x :: y :: xs)) =>
                      assert(
                        x.contains("Rescheduling") &&
                          y.contains("Giving up") &&
                          xs.size === MaxRetries &&
                          c === 1
                      )
                    case _ =>
                      fail(s"Expected $MaxRetries retries and reschedule")
                  }
                case _ =>
                  fail("Expected order error")
              }
          }
        }
      }
    }
  }
  test("failing to delete cart does not effect checkout") {
    implicit val bg = Background.NoOp
    import q2io.domain.Logger.NoOp
    forAll {
      (uid: UserId, pid: PaymentId, oid: OrderId, ct: CartTotal, card: Card) =>
        IOAssertion {
          new CheckoutProgram[IO](
            successfulClient(pid),
            failingCart(ct),
            successfulOrders(oid),
            retryPolicy
          ).checkout(uid, card)
            .map { id => assert(id == oid) }
        }
    }
  }

  test(s"successful checkout") {
    implicit val bg = Background.NoOp
    import q2io.domain.Logger.NoOp
    forAll {
      (uid: UserId, pid: PaymentId, oid: OrderId, ct: CartTotal, card: Card) =>
        IOAssertion {
          new CheckoutProgram[IO](
            successfulClient(pid),
            successfulCart(ct),
            successfulOrders(oid),
            retryPolicy
          ).checkout(uid, card)
            .map { id => assert(id == oid) }
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
