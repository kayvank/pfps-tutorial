package q2io.core.algebra

import cats.effect.Sync
import javax.crypto.spec.{PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, SecretKeyFactory}

import q2io.core.config.Config.PasswordSalt
import q2io.domain.Auth._

trait Crypto {
  def encrypt(value: Password): EncryptedPassword
  def decrypt(value: EncryptedPassword): Password
}

object Crypto {
  def apply[F[_]: Sync](secret: PasswordSalt): F[Crypto] =
    Sync[F]
      .delay {
        val salt = secret.value.value.value.getBytes("UTF-8")
        val keySpec = new PBEKeySpec("password".toCharArray(), salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val bytes = factory.generateSecret(keySpec).getEncoded
        val sKeySpec = new SecretKeySpec(bytes, "AES")
        val eCipher = EncryptCipher(Cipher.getInstance("AES"))
        eCipher.value.init(Cipher.ENCRYPT_MODE, sKeySpec)
        val dCipher = DecryptCipher(Cipher.getInstance("AES"))
        dCipher.value.init(Cipher.DECRYPT_MODE, sKeySpec)
        (eCipher, dCipher)
        // Workaround for PostgreSQL ERROR: invalid byte sequence for encoding "UTF8": 0x00
        val Key = "=DownInAHole="

        new Crypto {

          override def encrypt(password: Password): EncryptedPassword = {
            val bytes = password.value.getBytes("UTF-8")
            val result = new String(eCipher.value.doFinal(bytes), "UTF-8")
            val removeNull = result.replaceAll("\\u0000", Key)
            EncryptedPassword(removeNull)
          }

          override def decrypt(password: EncryptedPassword): Password = {
            val bytes = password.value.getBytes("UTF-8")
            val result = new String(dCipher.value.doFinal(bytes), "UTF-8")
            val insertNull = result.replaceAll(Key, "\\u0000")
            Password(insertNull)
          }
        }
      }
}
