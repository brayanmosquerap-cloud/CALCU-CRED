package com.example.persistence

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * MEDIO-06 — LOTE 1:
 * Proveedor de cifrado autenticado AEAD (`AES-256-GCM`) para proteger datos clínicos personales
 * y respuestas EDI en reposo dentro de la base de datos local Room.
 */
interface CredEdiCipherProvider {
    @Throws(GeneralSecurityException::class)
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray? = null): String

    @Throws(GeneralSecurityException::class)
    fun decrypt(ciphertextPayload: String, associatedData: ByteArray? = null): ByteArray
}

/**
 * Gestor criptográfico canónico para `CRED & EDI Antropometría 1.0`.
 *
 * - Algoritmo: `AES/GCM/NoPadding` (256 bits) con etiqueta de autenticación de 128 bits (`GCM_TAG_LENGTH_BITS = 128`)
 *   y vector de inicialización aleatorio de 96 bits (`GCM_IV_LENGTH_BYTES = 12`).
 * - Formato de carga cifrada codificada: `v1:<Base64(IV_12B || CIPHERTEXT_AND_TAG)>`.
 * - Integridad adicional: resumen criptográfico `SHA-256` del JSON canónico en claro para verificar post-descifrado.
 */
object CredEdiCryptoManager {
    const val PAYLOAD_PREFIX = "v1:"
    const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_IV_LENGTH_BYTES = 12
    const val GCM_TAG_LENGTH_BITS = 128
    const val DEFAULT_KEYSTORE_ALIAS = "cred_edi_clinical_master_key_v1"

    fun sha256Hex(input: String): String =
        sha256Hex(input.toByteArray(StandardCharsets.UTF_8))

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun generateRandom256BitAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        return keyGen.generateKey()
    }

    fun encodePayload(iv: ByteArray, ciphertextWithTag: ByteArray): String {
        require(iv.size == GCM_IV_LENGTH_BYTES) {
            "IV GCM inválido: se esperaban $GCM_IV_LENGTH_BYTES bytes y se recibieron ${iv.size}."
        }
        val combined = ByteBuffer.allocate(iv.size + ciphertextWithTag.size)
            .put(iv)
            .put(ciphertextWithTag)
            .array()
        return PAYLOAD_PREFIX + Base64.getEncoder().withoutPadding().encodeToString(combined)
    }

    @Throws(GeneralSecurityException::class)
    fun decodePayload(encoded: String): Pair<ByteArray, ByteArray> {
        if (!encoded.startsWith(PAYLOAD_PREFIX)) {
            throw GeneralSecurityException("Prefijo de versión de cifrado no reconocido o corrupto.")
        }
        val base64Part = encoded.removePrefix(PAYLOAD_PREFIX)
        val combined = try {
            Base64.getDecoder().decode(base64Part)
        } catch (e: IllegalArgumentException) {
            throw GeneralSecurityException("Carga cifrada Base64 malformada.", e)
        }
        if (combined.size <= GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
            throw GeneralSecurityException("Carga cifrada truncada o menor que el tamaño mínimo GCM.")
        }
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertextWithTag = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
        return iv to ciphertextWithTag
    }
}

/**
 * Proveedor AES-256-GCM respaldado por una clave simétrica en memoria/JVM.
 * Se utiliza en pruebas unitarias/Robolectric y como respaldo determinista cuando `AndroidKeyStore`
 * no está registrado en el entorno JVM de pruebas locales.
 */
class JvmAesGcmCipherProvider(
    private val secretKey: SecretKey = CredEdiCryptoManager.generateRandom256BitAesKey()
) : CredEdiCipherProvider {

    private val secureRandom = SecureRandom()

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray?): String {
        val iv = ByteArray(CredEdiCryptoManager.GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance(CredEdiCryptoManager.AES_TRANSFORMATION)
        val spec = GCMParameterSpec(CredEdiCryptoManager.GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        if (associatedData != null && associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData)
        }
        val ciphertext = cipher.doFinal(plaintext)
        return CredEdiCryptoManager.encodePayload(iv, ciphertext)
    }

    override fun decrypt(ciphertextPayload: String, associatedData: ByteArray?): ByteArray {
        val (iv, ciphertextWithTag) = CredEdiCryptoManager.decodePayload(ciphertextPayload)
        val cipher = Cipher.getInstance(CredEdiCryptoManager.AES_TRANSFORMATION)
        val spec = GCMParameterSpec(CredEdiCryptoManager.GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        if (associatedData != null && associatedData.isNotEmpty()) {
            cipher.updateAAD(associatedData)
        }
        return cipher.doFinal(ciphertextWithTag)
    }
}

/**
 * Proveedor AES-256-GCM respaldado exclusivamente por `AndroidKeyStore` en dispositivos Android reales/emulador.
 * Cumple regla crítica de seguridad (MEDIO-06 Lote 2, Sección 6):
 * - No sustituye la clave por una alternativa insegura si `AndroidKeyStore` falla.
 * - Lanza [GeneralSecurityException] ante cualquier fallo del almacén de claves del sistema.
 */
class AndroidKeyStoreAesGcmCipherProvider(
    private val keyAlias: String = CredEdiCryptoManager.DEFAULT_KEYSTORE_ALIAS
) : CredEdiCipherProvider {

    @Throws(GeneralSecurityException::class)
    private fun obtainOrCreateAndroidKeyStoreKey(): SecretKey {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
            if (existingKey != null) {
                return existingKey
            }
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
                ?: throw GeneralSecurityException("AndroidKeyStore devolvió una clave nula para el alias $keyAlias.")
        } catch (e: GeneralSecurityException) {
            throw e
        } catch (t: Throwable) {
            throw GeneralSecurityException("Fallo en AndroidKeyStore al obtener o generar la clave maestra AES-256-GCM.", t)
        }
    }

    override fun encrypt(plaintext: ByteArray, associatedData: ByteArray?): String {
        try {
            val hardwareKey = obtainOrCreateAndroidKeyStoreKey()
            val cipher = Cipher.getInstance(CredEdiCryptoManager.AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, hardwareKey)
            if (associatedData != null && associatedData.isNotEmpty()) {
                cipher.updateAAD(associatedData)
            }
            val ciphertext = cipher.doFinal(plaintext)
            val iv = cipher.iv
                ?: throw GeneralSecurityException("AndroidKeyStore no generó un IV válido para AES-GCM.")
            return CredEdiCryptoManager.encodePayload(iv, ciphertext)
        } catch (e: GeneralSecurityException) {
            throw e
        } catch (t: Throwable) {
            throw GeneralSecurityException("Error al cifrar con AndroidKeyStore AES-256-GCM.", t)
        }
    }

    override fun decrypt(ciphertextPayload: String, associatedData: ByteArray?): ByteArray {
        try {
            val hardwareKey = obtainOrCreateAndroidKeyStoreKey()
            val (iv, ciphertextWithTag) = CredEdiCryptoManager.decodePayload(ciphertextPayload)
            val cipher = Cipher.getInstance(CredEdiCryptoManager.AES_TRANSFORMATION)
            val spec = GCMParameterSpec(CredEdiCryptoManager.GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, hardwareKey, spec)
            if (associatedData != null && associatedData.isNotEmpty()) {
                cipher.updateAAD(associatedData)
            }
            return cipher.doFinal(ciphertextWithTag)
        } catch (e: GeneralSecurityException) {
            throw e
        } catch (t: Throwable) {
            throw GeneralSecurityException("Error al descifrar con AndroidKeyStore AES-256-GCM.", t)
        }
    }
}
