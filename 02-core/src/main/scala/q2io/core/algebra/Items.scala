package q2io.core
package algebra

import cats.effect._
import cats.syntax.all._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import squants.market._
import fs2.Stream

import q2io.domain.Brand.Brand
import q2io.domain.ext.Skunkx._
import q2io.domain.Category._
import q2io.domain.Brand._
import q2io.domain.Item._
import q2io.domain.effects.GenUUID

trait Items[F[_]] {
  def findAll: F[List[Item]]
  def findByN(brandName: BrandName, n: Int): F[PaginatedItems]
  def findBy(brandName: BrandName): F[List[Item]]
  def sfindBy(brandName: BrandName): Stream[F, Item]
  def findById(itemId: ItemId): F[Option[Item]]
  def create(item: CreateItem): F[Unit]
  def update(item: UpdateItem): F[Unit]
}

object Items {

  import ItemQueries._

  def apply[F[_]: Sync](sessionPool: Resource[F, Session[F]]): F[Items[F]] =
    Sync[F].delay(new Items[F] {
      override def findAll: F[List[Item]] =
        sessionPool.use(_.execute((selectAll)))

      override def findBy(brandName: BrandName): F[List[Item]] =
        sessionPool.use { session =>
          session.prepare(selectByBrand).use { ps =>
            ps.stream(brandName, 1024).compile.toList
          }
        }
      override def findByN(brandName: BrandName, n: Int): F[PaginatedItems] =
        sessionPool.use { session =>
          session.prepare(selectByBrand).use { ps =>
            ps.cursor(brandName).use { cs =>
              cs.fetch(n).map {
                case n ~ p => PaginatedItems(n, p)
              }
            }
          }
        }

      override def sfindBy(brandName: BrandName): Stream[F, Item] =
        for {
          sn <- fs2.Stream.resource(sessionPool)
          ps <- fs2.Stream.resource(sn.prepare(selectByBrand))
          rs <- ps.stream(brandName, 1024)
        } yield (rs)

      override def findById(itemId: ItemId): F[Option[Item]] =
        sessionPool.use({ session =>
          session.prepare(selectById).use { cs => cs.option(itemId) }
        })

      override def create(item: CreateItem): F[Unit] =
        sessionPool.use { session =>
          session.prepare(insertItem).use { cmd =>
            GenUUID[F].make[ItemId].flatMap { id =>
              cmd.execute(id ~ item).void
            }
          }
        }

      override def update(item: UpdateItem): F[Unit] =
        sessionPool.use { session =>
          session.prepare(updateItem).use { cmd => cmd.execute(item).void }
        }
    })
}

private object ItemQueries {

  val decoder: Decoder[Item] =
    (uuid ~ varchar ~ varchar ~ numeric ~ uuid ~ varchar ~ uuid ~ varchar).map {
      case i ~ n ~ d ~ p ~ bi ~ bn ~ ci ~ cn =>
        Item(
          ItemId(i),
          ItemName(n),
          ItemDescription(d),
          USD(p),
          Brand(BrandId(bi), BrandName(bn)),
          Category(CategoryId(ci), CategoryName(cn))
        )
    }
  val selectAll: Query[Void, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i
        INNER JOIN brands AS b ON i.brand_id = b.uuid
        INNER JOIN categories AS c ON i.category_id = c.uuid
       """.query(decoder)

  val selectByBrand: Query[BrandName, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i
        INNER JOIN brands AS b ON i.brand_id = b.uuid
        INNER JOIN categories AS c ON i.category_id = c.uuid
        WHERE b.name LIKE ${varchar.cimap[BrandName]}
       """.query(decoder)

  val selectById: Query[ItemId, Item] =
    sql"""
        SELECT i.uuid, i.name, i.description, i.price, b.uuid, b.name, c.uuid, c.name
        FROM items AS i
        INNER JOIN brands AS b ON i.brand_id = b.uuid
        INNER JOIN categories AS c ON i.category_id = c.uuid
        WHERE i.uuid = ${uuid.cimap[ItemId]}
       """.query(decoder)

  val insertItem: Command[ItemId ~ CreateItem] =
    sql"""
        INSERT INTO items
        VALUES ($uuid, $varchar, $varchar, $numeric, $uuid, $uuid)
       """.command.contramap {
      case id ~ i =>
        id.value ~ i.name.value ~ i.description.value ~ i.price.amount ~ i.brandId.value ~ i.categoryId.value
    }

  val updateItem: Command[UpdateItem] =
    sql"""
        UPDATE items
        SET price = $numeric
        WHERE uuid = ${uuid.cimap[ItemId]}
       """.command.contramap(i => i.price.amount ~ i.id)
}
