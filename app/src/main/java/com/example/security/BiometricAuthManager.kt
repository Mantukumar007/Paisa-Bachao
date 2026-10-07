package com.example.security

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import java.util.concurrent.Executor

enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNSUPPORTED
}

class BiometricAuthManager(private val context: Context) {

    private var cancellationSignal: CancellationSignal? = null

    fun checkBiometricStatus(): BiometricStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(Context.BIOMETRIC_SERVICE) as? android.hardware.biometrics.BiometricManager
                ?: return BiometricStatus.UNSUPPORTED

            return when (biometricManager.canAuthenticate(
                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )) {
                android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                else -> BiometricStatus.UNSUPPORTED
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return BiometricStatus.AVAILABLE
        }
        return BiometricStatus.UNSUPPORTED
    }

    fun promptBiometric(
        activity: Activity,
        executor: Executor,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError("Biometric authentication requires Android 9 (Pie) or higher.")
            return
        }

        cancellationSignal?.cancel()
        cancellationSignal = CancellationSignal()

        try {
            val builder = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                .setTitle("Unlock Paisa Bachao 🛡️")
                .setSubtitle("Biometric security for private financial records")
                .setDescription("Verify your fingerprint or face to protect your transactions, budgets, and savings.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ supports Biometric Strong or Device PIN/Pattern fallback
                builder.setAllowedAuthenticators(
                    android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                builder.setNegativeButton("Use 4-Digit PIN", executor) { _, _ ->
                    onError("Canceled. Please enter your 4-digit PIN.")
                }
            }

            val prompt = builder.build()
            prompt.authenticate(
                cancellationSignal!!,
                executor,
                object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        // User canceled or lockout
                        if (errorCode != android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                            errorCode != android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                            onError(errString?.toString() ?: "Authentication error. Enter PIN.")
                        }
                    }

                    override fun onAuthenticationFailed() {
                        // Biometric not recognized, user can retry on the same prompt
                    }
                }
            )
        } catch (e: Exception) {
            onError("Unable to launch BiometricPrompt: ${e.localizedMessage}")
        }
    }

    fun cancelAuthentication() {
        cancellationSignal?.cancel()
        cancellationSignal = null
    }
}
