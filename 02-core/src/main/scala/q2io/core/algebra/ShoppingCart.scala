package q2io.core.algebra

import cats.effect._
import cats.syntax.all._
import squants.market._
import dev.profunktor.redis4cats.RedisCommands

import q2io.domain.User._
import q2io.domain.Item._
import q2io.core.config.Config.ShoppingCartExpiration
import q2io.domain.effects.GenUUID
import q2io.domain.effects.Effects.{MonadThrow, ApThrow}
import q2io.domain.Cart._

trait ShoppingCart[F[_]] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def delete(userId: UserId): F[Unit]
  def removeItem(userId: UserId, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

object ShoppingCart {

  private def calcTotal(items: List[CartItem]): Money =
    USD(
      items.foldMap { i => i.item.price.value * i.quantity.value }
    )

  def apply[F[_]: Sync: GenUUID: MonadThrow](
      items: Items[F],
      redis: RedisCommands[F, String, String],
      exp: ShoppingCartExpiration
  ): F[ShoppingCart[F]] =
    Sync[F].delay(new ShoppingCart[F] {
      override def add(
          userId: UserId,
          itemId: ItemId,
          quantity: Quantity
      ): F[Unit] =
        redis.hSet(
          userId.value.toString,
          itemId.value.toString,
          quantity.value.toString
        ) *> redis.expire(userId.value.toString(), exp.value)

      override def get(userId: UserId): F[CartTotal] =
        redis.hGetAll(key = userId.value.toString).flatMap { it =>
          it.toList
            .traverseFilter {
              case (k, v) =>
                for {
                  id <- GenUUID[F].read[ItemId](k)
                  qt <- ApThrow[F].catchNonFatal(Quantity(v.toInt))
                  rs <- items.findById(id).map(_.map(i => CartItem(i, qt)))
                } yield (rs)
            }
            .map(items => CartTotal(items, calcTotal((items))))

        }

      override def delete(userId: UserId): F[Unit] =
        redis.del(userId.value.toString())

      override def removeItem(
          userId: UserId,
          itemId: ItemId
      ): F[Unit] = redis.hDel(userId.value.toString, itemId.value.toString())

      override def update(userId: UserId, cart: Cart): F[Unit] =
        redis.hGetAll(userId.value.toString).flatMap { it =>
          it.toList.traverse_ {
            case (k, _) =>
              GenUUID[F].read[ItemId](k).flatMap { id =>
                cart.items.get(id).traverse_ { q =>
                  redis.hSet(userId.value.toString, k, q.value.toString)
                }
              }
          } *> redis.expire(userId.value.toString, exp.value)
        }
    })
}
