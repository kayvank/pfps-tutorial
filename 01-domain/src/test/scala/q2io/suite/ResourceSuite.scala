package q2io.suite

import cats.effect._
import cats.effect.concurrent.Deferred
import org.scalatest.BeforeAndAfterAll
import org.scalactic.TripleEqualsSupport

trait ResourceSuite[A] extends PureTestSuite with BeforeAndAfterAll {

  def resources: Resource[IO, A]

  private[this] var res: A = _
  private[this] var cleanUp: IO[Unit] = _

  private[this] val latch = Deferred[IO, Unit].unsafeRunSync()

  override def beforeAll(): Unit = {
    super.beforeAll()
    val (r, h) = resources.allocated.unsafeRunSync()
    res = r
    cleanUp = h
    latch.complete(()).unsafeRunSync()
  }

  override def afterAll(): Unit = {
    cleanUp.unsafeRunSync()
    super.afterAll()
  }

  // ensure resource has been allocated before spec(...) bodies
  def withResources(f: (=> A) => Unit): Unit = f {
    latch.get.unsafeRunSync()
    res
  }
}
