package com.mark.moodlogger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Full-screen PIN lock, shown over everything else while the app is locked. No back
 * button, no swipe-away, no peeking at content behind it - see AppLockGate in
 * MainActivity.kt for how it's actually enforced at the app level.
 *
 * A correct PIN calls onUnlock. `onBiometric` is null when biometric unlock isn't
 * enabled or the phone doesn't support it - the fingerprint button just doesn't show.
 */
@Composable
fun LockScreen(
    onCheckPin: (String) -> Boolean,
    onUnlock: () -> Unit,
    onBiometric: (() -> Unit)?,
) {
    var entered by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    // Wrong-PIN feedback used to be invisible in practice (flagged by an outside
    // reviewer): clearing `entered` right after setting showError=true re-triggered this
    // same effect (keyed on `entered`), and the old `else` branch reset showError=false
    // on that very next run - so "Wrong PIN" and the red dots never actually stayed on
    // screen long enough to see. Clearing `entered` immediately is still correct (lets
    // the user retype right away); showError's own lifetime is now handled by the
    // separate timed effect below instead.
    LaunchedEffect(entered) {
        if (entered.length == 6) {
            if (onCheckPin(entered)) {
                onUnlock()
            } else {
                showError = true
                entered = ""
            }
        } else if (entered.isNotEmpty()) {
            // Starting a fresh attempt - dismiss the old error right away.
            showError = false
        }
    }

    // Real, visible feedback: "Wrong PIN" stays up for a moment on its own timer, not
    // tied to `entered`'s immediate reset, unless the block above already cleared it
    // because the user started typing again.
    LaunchedEffect(showError) {
        if (showError) {
            delay(900)
            showError = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text("Enter your PIN", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))

            // ---- the 6 dots showing how many digits are entered ----
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { i ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < entered.length) {
                                    if (showError) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                    )
                }
            }
            if (showError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Wrong PIN",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(32.dp))

            // ---- number pad ----
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (onBiometric != null) "bio" else "", "0", "del"),
            )
            rows.forEach { row ->
                Row {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (key) {
                                "" -> {}
                                "del" -> IconButton(onClick = { entered = entered.dropLast(1) }) {
                                    Icon(Icons.Default.Backspace, contentDescription = "Delete")
                                }
                                "bio" -> IconButton(onClick = { onBiometric?.invoke() }) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = "Use fingerprint")
                                }
                                else -> IconButton(
                                    onClick = { if (entered.length < 6) entered += key },
                                ) {
                                    Text(key, style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
