package cz.vanama.blesdk

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * A helper class for handling AES encryption and decryption.
 *
 * AES (Advanced Encryption Standard) is a symmetric encryption algorithm. This class
 * provides a simple way to encrypt and decrypt data using AES.
 *
 * Note: This class uses a fixed key for encryption and decryption. Ensure that the key
 * is stored safely and is not exposed to prevent security issues.
 *
 * @author Martin Vana
 */
class AesEncryptionHelper {

    private val encryptionKey = "aesEncryptionKey"
    private val charset = Charsets.UTF_8
    private val aes = "AES"

    private val secretKeySpec: SecretKeySpec
        get() = SecretKeySpec(encryptionKey.toByteArray(charset), aes)

    /**
     * Encrypts the given string using AES.
     *
     * @param strToEncrypt The string to encrypt.
     * @return The encrypted string, encoded in Base64.
     */
    fun encrypt(strToEncrypt: String): String {
        val cipher = Cipher.getInstance(aes)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        val encryptedString = Base64.encode(cipher.doFinal(strToEncrypt.toByteArray(charset)), Base64.DEFAULT)
        return String(encryptedString)
    }

    /**
     * Decrypts the given Base64 encoded, AES encrypted string.
     *
     * @param strToDecrypt The string to decrypt.
     * @return The decrypted string.
     */
    fun decrypt(strToDecrypt: String): String {
        val cipher = Cipher.getInstance(aes)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        val decryptedString = cipher.doFinal(Base64.decode(strToDecrypt.toByteArray(charset), Base64.DEFAULT))
        return String(decryptedString)
    }
}
