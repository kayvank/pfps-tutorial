package q2io.core.algebra

import cats.effect._
import cats.syntax.all._
import skunk._
import skunk.codec.all._
import skunk.implicits._

import q2io.domain.Auth._
import q2io.domain.User._
import q2io.domain.ext.Skunkx._
import q2io.domain.effects.Effects._
import q2io.core.protocol.json._
import q2io.domain.effects.GenUUID

trait Users[F[_]] {
  def find(username: UserName, password: Password): F[Option[User]]
  def create(username: UserName, password: Password): F[UserId]
}

import UserQueries._
object Users {

  def apply[F[_]: Sync](
      sessionPool: Resource[F, Session[F]],
      crypto: Crypto
  ): F[Users[F]] = Sync[F].delay(
    new Users[F] {
      override def find(
          username: UserName,
          password: Password
      ): F[Option[User]] =
        sessionPool.use { session =>
          session.prepare(selectUser).use { q =>
            q.option(username).map {
              case Some(u ~ p) if p.value == crypto.encrypt(password).value =>
                u.some
              case _ => none[User]
            }
          }
        }

      override def create(username: UserName, password: Password): F[UserId] =
        sessionPool.use {
          session =>
            session.prepare(insertUser).use { cmd =>
              GenUUID[F].make[UserId].flatMap { id =>
                cmd
                  .execute(User(id, username) ~ crypto.encrypt(password))
                  .as(id)
                  .handleErrorWith {
                    case SqlState.UniqueViolation(_) =>
                      UserNameInUse(username).raiseError[F, UserId]
                  }
              }
            }
        }
    }
  )
}
object UserQueries {
  val codec: Codec[User ~ EncryptedPassword] =
    (uuid.cimap[UserId] ~ varchar.cimap[UserName] ~ varchar
      .cimap[EncryptedPassword]).imap {
      case i ~ n ~ p => User(i, n) ~ p
    } {
      case u ~ p => u.id ~ u.name ~ p
    }

  val selectUser: Query[UserName, User ~ EncryptedPassword] =
    sql"""
        SELECT * FROM users
        WHERE name = ${varchar.cimap[UserName]}
       """.query(codec)

  val insertUser: Command[User ~ EncryptedPassword] =
    sql"""
            INSERT INTO users
            VALUES ($codec)
        """.command
}
