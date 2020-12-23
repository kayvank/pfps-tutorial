package q2io.http.routes

import cats.effect._
import cats.syntax.option._
import org.http4s._
import org.http4s.Method._
import org.http4s.client.dsl.io._
import fs2.Stream

import q2io.core.protocol.json._
import q2io.domain.Arbitraries._

import q2io.domain.Brand._
import q2io.domain.Item._
import q2io.core.algebra.Items
import q2io.suite._

class ItemRoutesSpec extends HttpTestSuite {

  def dataItems(items: List[Item]) = new TestItems {
    override def findAll: IO[List[Item]] = IO.pure(items)
  }

  def failingItems(items: List[Item]) = new TestItems {
    override def findAll: IO[List[Item]] =
      IO.raiseError(DummyError) *> IO.pure(items)
    override def findBy(brand: BrandName): IO[List[Item]] = findAll
  }

  test("GET items [OK]") {
    forAll { (it: List[Item]) =>
      IOAssertion {
        GET(Uri.uri("/items")).flatMap { req =>
          val routes = new ItemRoutes[IO](dataItems(it)).routes
          assertHttp(routes, req)(Status.Ok, it)
        }
      }
    }
  }

  test("GET items by Brand [OK]") {
    forAll { (it: List[Item], b: Brand) =>
      IOAssertion {
        GET(Uri.uri("/items").withQueryParam(b.name.value)).flatMap { req =>
          val routes = new ItemRoutes[IO](dataItems(it)).routes
          assertHttp(routes, req)(Status.Ok, it)
        }
      }
    }
  }

  test("GET items [ERROR]") {
    forAll { (it: List[Item]) =>
      IOAssertion {
        GET(Uri.uri("/items")).flatMap { req =>
          val routes = new ItemRoutes[IO](failingItems(it)).routes
          assertHttpFailure(routes, req)
        }
      }
    }
  }
}

protected class TestItems() extends Items[IO] {
  def findAll: IO[List[Item]] = IO.pure(List.empty)
  def findBy(brand: BrandName): IO[List[Item]] = IO.pure(List.empty)
  def findByN(brand: BrandName, n: Int): IO[PaginatedItems] =
    IO.pure(PaginatedItems(List.empty, false))
  def findById(itemId: ItemId): IO[Option[Item]] = IO.pure(none[Item])
  def sfindBy(brand: BrandName): Stream[IO, Item] = Stream.empty
  def create(item: CreateItem): IO[Unit] = IO.unit
  def update(item: UpdateItem): IO[Unit] = IO.unit
}
