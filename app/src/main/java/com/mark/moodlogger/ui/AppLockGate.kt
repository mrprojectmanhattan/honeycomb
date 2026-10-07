package com.mark.moodlogger.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Wraps the real app content and shows LockScreen over top of it until unlocked. Locks
 * again automatically whenever the whole app (not just one screen) goes to the
 * background and comes back - ProcessLifecycleOwner, not this Activity's own lifecycle,
 * so switching between Honeycomb's own screens never re-triggers it, only leaving the
 * app entirely does.
 */
@Composable
fun AppLockGate(
    lockEnabled: Boolean,
    checkPin: (String) -> Boolean,
    biometricEnabled: Boolean,
    requestBiometric: (onSuccess: () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    var locked by remember(lockEnabled) { mutableStateOf(lockEnabled) }

    DisposableEffect(lockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && lockEnabled) locked = true
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    if (locked) {
        LockScreen(
            onCheckPin = checkPin,
            onUnlock = { locked = false },
            onBiometric = if (biometricEnabled) {
                { requestBiometric { locked = false } }
            } else null,
        )
    } else {
        content()
    }
}
