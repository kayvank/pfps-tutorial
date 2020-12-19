package q2io.core
package algebra

import cats.effect._
import cats.syntax.all._
import skunk._
import skunk.codec.all._
import skunk.implicits._

import q2io.domain.ext.Skunkx._
import q2io.domain.Category._
import q2io.domain.effects.GenUUID

trait Categories[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[Unit]
}

import CategoryQuerie._
object Categories {
  def apply[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ) = Sync[F].delay(
    new Categories[F] {
      override def findAll = sessionPool.use(_.execute(selectAll))
      override def create(name: CategoryName): F[Unit] =
        sessionPool.use { session =>
          session.prepare(insertCategory).use { cmd =>
            GenUUID[F].make[CategoryId].flatMap { id =>
              cmd.execute(Category(id, name)).void
            }
          }
        }
    }
  )
}

private object CategoryQuerie {
  val codec: Codec[Category] =
    (uuid.cimap[CategoryId] ~ varchar.cimap[CategoryName]).imap {
      case i ~ n => Category(i, n)
    }(c => c.uuid ~ c.name)

  val selectAll: Query[Void, Category] =
    sql"""
        SELECT * FROM categories
       """.query(codec)
  val insertCategory: Command[Category] =
    sql"""
        INSERT INTO categories
        VALUES($codec)
        """.command
}
