package com.ilyne.helloszigetkmp.presentation.component.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.component.actionbutton.ActionButton
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.common_dismiss
import hello_sziget_kmp.shared.generated.resources.common_retry
import org.jetbrains.compose.resources.stringResource

/**
 * Centered error message for the common "screen failed to load" state.
 *
 * When [onRetry] is non-null, a "Retry" [ActionButton] (label configurable via [retryLabel]) is
 * shown below the message. When [onDismiss] is non-null, a "Dismiss" text button is shown
 * alongside it, for screens that surface this as a non-blocking banner over otherwise-usable
 * (e.g. cached) content rather than a full-screen replacement. Screens that aren't refreshable
 * or dismissible (or that surface retry another way) can leave the corresponding parameter null.
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    retryLabel: String = stringResource(Res.string.common_retry),
    dismissLabel: String = stringResource(Res.string.common_dismiss),
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (onRetry != null || onDismiss != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onRetry != null) {
                        ActionButton(text = retryLabel, onClick = onRetry)
                    }
                    if (onDismiss != null) {
                        TextButton(onClick = onDismiss) {
                            Text(text = dismissLabel)
                        }
                    }
                }
            }
        } else {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
