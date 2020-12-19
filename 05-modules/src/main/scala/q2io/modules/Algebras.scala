package q2io.modules

import cats.Parallel
import cats.effect._
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import skunk._

import q2io.core.config.Config._
import q2io.core.algebra.ShoppingCart
import q2io.core.algebra.Brands
import q2io.core.algebra.HealthCheck
import q2io.core.algebra.Orders
import q2io.core.algebra.Items
import q2io.core.algebra.Categories

final class Algebras[F[_]] private (
    val cart: ShoppingCart[F],
    val brands: Brands[F],
    val categories: Categories[F],
    val orders: Orders[F],
    val items: Items[F],
    val healthCheck: HealthCheck[F]
)
object Algebras {
  def apply[F[_]: Concurrent: Parallel: Timer](
      redis: RedisCommands[F, String, String],
      sessionPool: Resource[F, Session[F]],
      cartExpiration: ShoppingCartExpiration
  ): F[Algebras[F]] =
    for {
      brands <- Brands.apply[F](sessionPool)
      categories <- Categories[F](sessionPool)
      items <- Items[F](sessionPool)
      cart <- ShoppingCart.apply[F](items, redis, cartExpiration)
      orders <- Orders.apply[F](sessionPool)
      healthCheck <- HealthCheck.apply[F](sessionPool, redis)
    } yield (new Algebras[F](
      cart,
      brands,
      categories,
      orders,
      items,
      healthCheck
    ))

}
