package q2io.domain

import cats.effect.IO
import cats.effect.concurrent.Ref
import q2io.domain.effects.Background
import scala.concurrent.duration.FiniteDuration

object Background {

  val NoOp: Background[IO] =
    new Background[IO] {
      def schedule[A](fa: IO[A], duration: FiniteDuration): IO[Unit] = IO.unit
    }

  def counter(ref: Ref[IO, Int]): Background[IO] =
    new Background[IO] {
      def schedule[A](fa: IO[A], duration: FiniteDuration): IO[Unit] =
        ref.update(_ + 1)
    }

}
