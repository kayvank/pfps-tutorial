package q2io.core
package algebra

import q2io.domain.Brand._
import q2io.domain.effects._
import q2io.domain.ext.Skunkx._

import cats.effect._
import cats.syntax.all._
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[Unit]
}
object Brands {
  import BrandQueries._

  /* BracketThrow[F[_]] is just a type alias for Bracket[F, Throwable] */
  def apply[F[_]: Sync: BracketThrow: GenUUID](
      sessionPool: Resource[F, Session[F]]
  ): F[Brands[F]] =
    Sync[F].delay(new Brands[F] {
      override def findAll: F[List[Brand]] =
        sessionPool.use(_.execute((selectAll)))

      override def create(name: BrandName): F[Unit] = sessionPool.use {
        session =>
          session.prepare(insertBrand).use { cmd =>
            GenUUID[F].make[BrandId].flatMap { id =>
              cmd.execute(Brand(id, name)).void
            }
          }
      }
    })
}

private object BrandQueries {

  val codec: Codec[Brand] =
    (uuid.cimap[BrandId] ~ varchar.cimap[BrandName]).imap {
      case i ~ n => Brand(i, n)
    }(b => b.uuid ~ b.name)

  val selectAll: Query[Void, Brand] =
    sql"""
        SELECT * FROM brands
       """.query(codec)

  val insertBrand: Command[Brand] =
    sql"""
        INSERT INTO brands
        VALUES ($codec)
      """.command

}
