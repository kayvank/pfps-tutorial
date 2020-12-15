package q2io.domain

import io.estatico.newtype.macros._

object Healthcheck {
  @newtype case class RedisStatus(value: Boolean)
  @newtype case class PostgresStatus(value: Boolean)

  case class AppStatus(
      redis: RedisStatus,
      postgres: PostgresStatus
  )
}
