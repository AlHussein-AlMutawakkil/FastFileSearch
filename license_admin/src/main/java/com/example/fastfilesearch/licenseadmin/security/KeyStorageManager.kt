package com.example.fastfilesearch.licenseadmin.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object KeyStorageManager {

    const val EMBEDDED_PUBLIC_KEY_BASE64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAERt37fPahuYV+EG8Jr9zMBgYjxJdgjKaKq5CFNjVrFve5EL7N7lB1zDAqtyNniG2NkPGZbzWbLVBx1ZwjCaM5HQ=="

    private const val PREFS_NAME = "admin_secure_key_prefs"
    private const val KEY_CIPHERTEXT = "encrypted_private_key"
    private const val KEY_IV = "gcm_iv"
    private const val KEYSTORE_ALIAS = "AdminLicenseMasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    private val VALIDATION_CHALLENGE = "FAST_FILE_SEARCH_LICENSE_VALIDATION".toByteArray(StandardCharsets.UTF_8)

    /**
     * Checks whether an encrypted private key is currently saved.
     */
    fun hasKey(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_CIPHERTEXT) && prefs.contains(KEY_IV)
    }

    /**
     * Deletes the stored encrypted private key.
     */
    fun deleteKey(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.edit().remove(KEY_CIPHERTEXT).remove(KEY_IV).commit()
    }

    /**
     * Imports and validates a private key PEM from an InputStream, then encrypts and stores it.
     */
    fun importKeyFromStream(context: Context, inputStream: InputStream): Result<Unit> {
        return try {
            val content = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            importKeyFromPem(context, content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports, parses, verifies against the embedded public key, and securely encrypts the private key.
     */
    fun importKeyFromPem(context: Context, pemContent: String): Result<Unit> {
        return try {
            val cleanContent = pemContent.trim()
            if (cleanContent.isBlank()) {
                return Result.failure(IllegalArgumentException("محتوى المفتاح فارغ"))
            }

            val privateKey = parsePrivateKeyFromPem(cleanContent)

            // Verify that this private key signs data that matches the embedded public key
            val isValid = verifyKeyMatch(privateKey, EMBEDDED_PUBLIC_KEY_BASE64)
            if (!isValid) {
                return Result.failure(
                    IllegalArgumentException("المفتاح الخاص المستورد لا يطابق المفتاح العام لتطبيق «بحث آمن». يرجى التأكد من اختيار الملف الصحيح.")
                )
            }

            // Encrypt and persist
            val keyBytes = privateKey.encoded
            encryptAndStoreKeyBytes(context, keyBytes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves and decrypts the private key.
     */
    fun getPrivateKey(context: Context): PrivateKey? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ciphertextB64 = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val ivB64 = prefs.getString(KEY_IV, null) ?: return null

        return try {
            val ciphertext = decodeBase64(ciphertextB64)
            val iv = decodeBase64(ivB64)

            val decryptedBytes = decryptBytes(context, ciphertext, iv)
            val keySpec = PKCS8EncodedKeySpec(decryptedBytes)
            val keyFactory = KeyFactory.getInstance("EC")
            keyFactory.generatePrivate(keySpec)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses PKCS#8 private key from PEM string.
     */
    fun parsePrivateKeyFromPem(pem: String): PrivateKey {
        // Remove PEM headers and line breaks
        val cleanBase64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN EC PRIVATE KEY-----", "")
            .replace("-----END EC PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")
            .trim()

        if (cleanBase64.isEmpty()) {
            throw IllegalArgumentException("تنسيق ملف المفتاح غير صالح (لا يحتوي على ترميز Base64)")
        }

        val decoded = decodeBase64(cleanBase64)
        val spec = PKCS8EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePrivate(spec)
    }

    /**
     * Cryptographically verifies that a private key corresponds to a public key by signing and verifying a challenge.
     */
    fun verifyKeyMatch(privateKey: PrivateKey, publicKeyBase64: String): Boolean {
        return try {
            val pubBytes = decodeBase64(publicKeyBase64)
            val pubSpec = X509EncodedKeySpec(pubBytes)
            val kf = KeyFactory.getInstance("EC")
            val publicKey: PublicKey = kf.generatePublic(pubSpec)

            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initSign(privateKey)
            signer.update(VALIDATION_CHALLENGE)
            val signature = signer.sign()

            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(VALIDATION_CHALLENGE)
            verifier.verify(signature)
        } catch (_: Exception) {
            false
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        val clean = str.replace("\\s+".toRegex(), "").trim()
        return try {
            Base64.getDecoder().decode(clean)
        } catch (_: Exception) {
            android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
        }
    }

    private fun encodeBase64(data: ByteArray): String {
        return try {
            Base64.getEncoder().encodeToString(data)
        } catch (_: Exception) {
            android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
        }
    }

    private fun getOrCreateMasterKey(context: Context): SecretKey {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                val key = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
                if (key != null) {
                    return key
                }
            }

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (_: Exception) {
            // Fallback for environments or devices where AndroidKeyStore has issues
            return getFallbackAppKey(context)
        }
    }

    private fun getFallbackAppKey(context: Context): SecretKey {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedKey = prefs.getString("fallback_master_seed", null)
        if (savedKey != null) {
            val raw = decodeBase64(savedKey)
            return SecretKeySpec(raw, "AES")
        }
        val randomBytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(randomBytes)
        prefs.edit().putString("fallback_master_seed", encodeBase64(randomBytes)).apply()
        return SecretKeySpec(randomBytes, "AES")
    }

    private fun encryptAndStoreKeyBytes(context: Context, keyBytes: ByteArray) {
        val masterKey = getOrCreateMasterKey(context)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(keyBytes)

        val ciphertextB64 = encodeBase64(ciphertext)
        val ivB64 = encodeBase64(iv)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CIPHERTEXT, ciphertextB64)
            .putString(KEY_IV, ivB64)
            .apply()
    }

    private fun decryptBytes(context: Context, ciphertext: ByteArray, iv: ByteArray): ByteArray {
        val masterKey = getOrCreateMasterKey(context)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
        return cipher.doFinal(ciphertext)
    }
}
