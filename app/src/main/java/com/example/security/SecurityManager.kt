package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("paisa_bachao_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_PIN_HASH = "pref_pin_hash"
        private const val PREF_PIN_SALT = "pref_pin_salt"
        private const val PREF_LOCK_ENABLED = "pref_lock_enabled"
        private const val PREF_BIOMETRIC_ENABLED = "pref_biometric_enabled"
        private const val PREF_SMS_AUTO_SCAN = "pref_sms_auto_scan"
        private const val PREF_SMS_PERMISSION_GRANTED = "pref_sms_permission_granted"
        private const val PREF_MASK_BALANCE = "pref_mask_balance"
        private const val PREF_USER_CONSENTED = "pref_user_consented"
        private const val PREF_CONSENT_TIMESTAMP = "pref_consent_timestamp"

        // 256-bit encryption key derived deterministically for local at-rest encryption
        private val LOCAL_AES_KEY_BYTES = byteArrayOf(
            0x41, 0x6E, 0x64, 0x72, 0x6F, 0x69, 0x64, 0x50,
            0x61, 0x69, 0x73, 0x61, 0x42, 0x61, 0x63, 0x68,
            0x61, 0x6F, 0x53, 0x65, 0x63, 0x75, 0x72, 0x65,
            0x4B, 0x65, 0x79, 0x32, 0x30, 0x32, 0x36, 0x21
        )
    }

    // Hash helper
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    // Encrypt text using AES-GCM
    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        return try {
            val key = SecretKeySpec(LOCAL_AES_KEY_BYTES, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fallback base64
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    // Decrypt text
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isBlank()) return ""
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            val iv = ByteArray(12) // standard GCM IV length
            if (combined.size < 12) return encryptedText
            System.arraycopy(combined, 0, iv, 0, 12)
            val cipherBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, cipherBytes, 0, cipherBytes.size)

            val key = SecretKeySpec(LOCAL_AES_KEY_BYTES, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                String(Base64.decode(encryptedText, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (ex: Exception) {
                encryptedText
            }
        }
    }

    // PIN Authentication
    fun isAppLockEnabled(): Boolean = prefs.getBoolean(PREF_LOCK_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_LOCK_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(PREF_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun hasPinSet(): Boolean = prefs.contains(PREF_PIN_HASH)

    fun setPin(pin: String) {
        val salt = System.currentTimeMillis().toString()
        val hash = sha256("$salt:$pin")
        prefs.edit()
            .putString(PREF_PIN_SALT, salt)
            .putString(PREF_PIN_HASH, hash)
            .putBoolean(PREF_LOCK_ENABLED, true)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(PREF_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(PREF_PIN_HASH, null) ?: return false
        val testHash = sha256("$salt:$pin")
        return testHash == storedHash
    }

    fun clearPin() {
        prefs.edit()
            .remove(PREF_PIN_SALT)
            .remove(PREF_PIN_HASH)
            .putBoolean(PREF_LOCK_ENABLED, false)
            .apply()
    }

    // SMS Privacy Settings
    fun isSmsAutoScanEnabled(): Boolean = prefs.getBoolean(PREF_SMS_AUTO_SCAN, true)

    fun setSmsAutoScanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_SMS_AUTO_SCAN, enabled).apply()
    }

    fun isSmsPermissionGranted(): Boolean = prefs.getBoolean(PREF_SMS_PERMISSION_GRANTED, false)

    fun setSmsPermissionGranted(granted: Boolean) {
        prefs.edit().putBoolean(PREF_SMS_PERMISSION_GRANTED, granted).apply()
    }

    // Mask balance privacy toggle
    fun isBalanceMasked(): Boolean = prefs.getBoolean(PREF_MASK_BALANCE, false)

    fun setBalanceMasked(masked: Boolean) {
        prefs.edit().putBoolean(PREF_MASK_BALANCE, masked).apply()
    }

    // First-run Privacy Policy & Terms Consent
    fun hasUserConsented(): Boolean = prefs.getBoolean(PREF_USER_CONSENTED, false)

    fun setUserConsented(consented: Boolean) {
        prefs.edit()
            .putBoolean(PREF_USER_CONSENTED, consented)
            .putLong(PREF_CONSENT_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getConsentTimestamp(): Long = prefs.getLong(PREF_CONSENT_TIMESTAMP, 0L)
}
