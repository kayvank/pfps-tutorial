package q2io.core
package config

import enumeratum.{CirisEnum, EnumEntry, Enum}

object Environments {
  sealed abstract class AppEnvironment extends EnumEntry
  object AppEnvironment
      extends Enum[AppEnvironment]
      with CirisEnum[AppEnvironment] {
    case object Local extends AppEnvironment
    case object Test extends AppEnvironment
    case object Prod extends AppEnvironment

    val values = findValues
  }

}
