package pl.summernote.summernote.customs

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AccessTokenManager(private val context: Context) {
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private val alias = "my_access_token"
    private val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")

    init {
        keyStore.load(null)
        if (!keyStore.containsAlias(alias)) {
            createNewKey()
        }
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
    }

    private fun createNewKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setKeySize(256)
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setRandomizedEncryptionRequired(false)
            build()
        }
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getKey(): SecretKey {
        return keyStore.getKey(alias, null) as SecretKey
    }

    fun saveToken(token: String) {
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        val sharedPreferences = context.getSharedPreferences("access_token", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("token", encoded)
            commit()
        }
    }

    fun getToken(): String? {
        val sharedPreferences = context.getSharedPreferences("access_token", Context.MODE_PRIVATE)
        val encoded = sharedPreferences.getString("token", null)
        if (encoded != null) {
            val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        }
        return null
    }

    fun deleteToken() {
        val sharedPreferences = context.getSharedPreferences("access_token", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("token")
            commit()
        }
    }
}

