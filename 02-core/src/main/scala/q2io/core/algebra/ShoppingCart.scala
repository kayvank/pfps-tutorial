package q2io.core.algebra

import cats.effect._
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import q2io.domain.effects.Effects
import q2io.domain.User.UserId
import q2io.domain.Item.ItemId
import q2io.domain.Cart.{Quantity, CartTotal, Cart}
import q2io.core.config.Config.ShoppingCartExpiration
import q2io.domain.effects.GenUUID
import q2io.domain.effects.Effects.MonadThrow

trait ShoppingCart[F[_]] {
  def add(userId: UserId, itemId: ItemId, quantity: Quantity): F[Unit]
  def get(userId: UserId): F[CartTotal]
  def delete(userId: UserId): F[Unit]
  def removeItem(userId: UserId, cart: Cart, itemId: ItemId): F[Unit]
  def update(userId: UserId, cart: Cart): F[Unit]
}

object ShoppingCart {
  def apply[F[_]: Sync: GenUUID: MonadThrow](
      items: Items[F],
      redis: ShoppingCartExpiration
  ): F[ShoppingCart[F]] =
    Sync[F].delay(new ShoppingCart[F] {
      override def add(
          userId: UserId,
          itemId: ItemId,
          quantity: Quantity
      ): F[Unit] = ???

      override def get(userId: UserId): F[CartTotal] = ???

      override def delete(userId: UserId): F[Unit] = ???

      override def removeItem(
          userId: UserId,
          cart: Cart,
          itemId: ItemId
      ): F[Unit] = ???

      override def update(userId: UserId, cart: Cart): F[Unit] = ???
    })
}
