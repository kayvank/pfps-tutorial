package q2io.core.protocol

import cats.Applicative
import dev.profunktor.auth.jwt.JwtToken
import io.circe._
import io.circe.generic.semiauto._
import io.circe.refined._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import squants.market.{Money, USD}

import q2io.domain.Auth._
import q2io.domain.Brand._
import q2io.domain.Cart._
import q2io.domain.Category._
import q2io.domain.Checkout._
import q2io.domain.Healthcheck._
import q2io.domain.Item._
import q2io.domain.Order._
import q2io.domain.Payment._
import q2io.domain.ext.Refined._
import q2io.domain.User._

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]
      : EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

trait JsonCodecs {

  // ----- Overriding some Coercible codecs ----
  implicit val brandParamDecoder: Decoder[BrandParam] =
    Decoder.forProduct1("name")(BrandParam.apply)

  implicit val categoryParamDecoder: Decoder[CategoryParam] =
    Decoder.forProduct1("name")(CategoryParam.apply)

  implicit val paymentIdDecoder: Decoder[PaymentId] =
    Decoder.forProduct1("paymentId")(PaymentId.apply)

  // ----- Coercible codecs -----
  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.repr.asInstanceOf[B])

  implicit def coercibleKeyDecoder[A: Coercible[B, *], B: KeyDecoder]
      : KeyDecoder[A] =
    KeyDecoder[B].map(_.coerce[A])

  implicit def coercibleKeyEncoder[A: Coercible[B, *], B: KeyEncoder]
      : KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.repr.asInstanceOf[B])

  // ----- Domain codecs -----

  implicit val brandDecoder: Decoder[Brand] = deriveDecoder[Brand]
  implicit val brandEncoder: Encoder[Brand] = deriveEncoder[Brand]

  implicit val categoryDecoder: Decoder[Category] = deriveDecoder[Category]
  implicit val categoryEncoder: Encoder[Category] = deriveEncoder[Category]

  implicit val moneyDecoder: Decoder[Money] =
    Decoder[BigDecimal].map(USD.apply)

  implicit val moneyEncoder: Encoder[Money] =
    Encoder[BigDecimal].contramap(_.amount)

  implicit val itemDecoder: Decoder[Item] = deriveDecoder[Item]
  implicit val itemEncoder: Encoder[Item] = deriveEncoder[Item]

  implicit val createItemDecoder: Decoder[CreateItemParam] =
    deriveDecoder[CreateItemParam]
  implicit val updateItemDecoder: Decoder[UpdateItemParam] =
    deriveDecoder[UpdateItemParam]

  implicit val cartItemDecoder: Decoder[CartItem] = deriveDecoder[CartItem]
  implicit val cartItemEncoder: Encoder[CartItem] = deriveEncoder[CartItem]

  implicit val cartTotalEncoder: Encoder[CartTotal] = deriveEncoder[CartTotal]

  implicit val tokenEncoder: Encoder[JwtToken] =
    Encoder.forProduct1("access_token")(_.value)

  implicit val cartEncoder: Encoder[Cart] =
    Encoder.forProduct1("items")(_.items)

  implicit val cartDecoder: Decoder[Cart] =
    Decoder.forProduct1("items")(Cart.apply)

  implicit val userDecoder: Decoder[User] = deriveDecoder[User]
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]

  implicit val cardEncoder: Encoder[Card] = deriveEncoder[Card]
  implicit val cardDecoder: Decoder[Card] = deriveDecoder[Card]

  implicit val paymentEncoder: Encoder[Payment] = deriveEncoder[Payment]

  implicit val orderEncoder: Encoder[Order] = deriveEncoder[Order]
  implicit val orderDecoder: Decoder[Order] = deriveDecoder[Order]

  implicit val appStatusEncoder: Encoder[AppStatus] = deriveEncoder[AppStatus]

  implicit val createUserDecoder: Decoder[CreateUser] =
    deriveDecoder[CreateUser]

  implicit val loginUserDecoder: Decoder[LoginUser] = deriveDecoder[LoginUser]

}
