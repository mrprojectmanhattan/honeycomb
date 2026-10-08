package com.mark.moodlogger.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mark.moodlogger.data.AppLock
import com.mark.moodlogger.notification.Notifications
import com.mark.moodlogger.ui.theme.MoodLoggerTheme

/**
 * FragmentActivity, not ComponentActivity, as of the app-lock work (2026-09-28) -
 * androidx.biometric.BiometricPrompt requires it. See NUTRITION_MERGE_NOTES.md /
 * the app-lock section for why, if this looks like an odd base class to land on.
 */
class MainActivity : FragmentActivity() {

    private val vm: MainViewModel by viewModels { MainViewModel.Factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startOnLog = intent?.getBooleanExtra(Notifications.EXTRA_FROM_NOTIFICATION, false) == true
        val startOnTimer = intent?.getBooleanExtra(Notifications.EXTRA_OPEN_TIMER, false) == true
        val startOnShopping = intent?.getBooleanExtra(Notifications.EXTRA_OPEN_SHOPPING, false) == true
        val startOnJournalRecap = intent?.getBooleanExtra(Notifications.EXTRA_OPEN_JOURNAL_RECAP, false) == true
        val startOnCare = intent?.getBooleanExtra(Notifications.EXTRA_OPEN_CARE, false) == true
        setContent {
            MoodLoggerTheme {
                val settings by vm.settings.collectAsState()
                val settingsReady by vm.settingsReady.collectAsState()
                AppLockGate(
                    settingsReady = settingsReady,
                    lockEnabled = settings.appLockEnabled,
                    checkPin = { pin ->
                        when {
                            AppLock.verifyPin(pin, settings.appLockPinHash, settings.appLockSalt) -> true
                            AppLock.verifyLegacyPin(pin, settings.appLockPinHash, settings.appLockSalt) -> {
                                // Correct PIN, just stored under the pre-2026-10-07 scheme -
                                // upgrade it silently so this path never fires again.
                                vm.upgradeLegacyPin(pin)
                                true
                            }
                            else -> false
                        }
                    },
                    biometricEnabled = settings.appLockBiometricEnabled,
                    requestBiometric = { onSuccess -> showBiometricPrompt(onSuccess) },
                ) {
                    AppRoot(
                        vm = vm,
                        startOnLog = startOnLog,
                        startOnTimer = startOnTimer,
                        startOnShopping = startOnShopping,
                        startOnJournalRecap = startOnJournalRecap,
                        startOnCare = startOnCare,
                    )
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                // Errors/failures just leave the PIN pad up - no extra handling needed.
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Honeycomb")
            .setNegativeButtonText("Use PIN instead")
            .build()
        prompt.authenticate(info)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val fromMood = intent.getBooleanExtra(Notifications.EXTRA_FROM_NOTIFICATION, false)
        val fromTimer = intent.getBooleanExtra(Notifications.EXTRA_OPEN_TIMER, false)
        val fromShopping = intent.getBooleanExtra(Notifications.EXTRA_OPEN_SHOPPING, false)
        val fromJournalRecap = intent.getBooleanExtra(Notifications.EXTRA_OPEN_JOURNAL_RECAP, false)
        val fromCare = intent.getBooleanExtra(Notifications.EXTRA_OPEN_CARE, false)
        if (fromMood || fromTimer || fromShopping || fromJournalRecap || fromCare) {
            // Relaunch composition so it opens straight on the right screen.
            recreate()
        }
    }
}
