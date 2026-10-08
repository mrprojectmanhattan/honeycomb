package com.mark.moodlogger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Wraps the real app content and shows LockScreen over top of it until unlocked. Locks
 * again automatically whenever the whole app (not just one screen) goes to the
 * background and comes back - ProcessLifecycleOwner, not this Activity's own lifecycle,
 * so switching between Honeycomb's own screens never re-triggers it, only leaving the
 * app entirely does.
 *
 * Fail-closed by design (2026-10-07, flagged by an outside reviewer): `locked` always
 * starts `true`, never `lockEnabled` directly. On a cold start, [settingsReady] is false
 * for a brief moment before the real settings load from disk - trusting `lockEnabled`
 * during that window meant it carried the Settings() placeholder default
 * (appLockEnabled = false), which briefly rendered real app content before the real
 * lock state arrived. Until settingsReady is true, this shows a neutral blank screen -
 * never the real content, and not even the PIN pad before we actually know a PIN is
 * required.
 */
@Composable
fun AppLockGate(
    settingsReady: Boolean,
    lockEnabled: Boolean,
    checkPin: (String) -> Boolean,
    biometricEnabled: Boolean,
    requestBiometric: (onSuccess: () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    var locked by remember { mutableStateOf(true) }

    LaunchedEffect(settingsReady, lockEnabled) {
        if (settingsReady) locked = lockEnabled
    }

    DisposableEffect(lockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && lockEnabled) locked = true
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    when {
        !settingsReady -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        locked -> LockScreen(
            onCheckPin = checkPin,
            onUnlock = { locked = false },
            onBiometric = if (biometricEnabled) {
                { requestBiometric { locked = false } }
            } else null,
        )
        else -> content()
    }
}
