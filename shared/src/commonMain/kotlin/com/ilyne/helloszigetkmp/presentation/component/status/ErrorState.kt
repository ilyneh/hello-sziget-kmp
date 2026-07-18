package com.ilyne.helloszigetkmp.presentation.component.status

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.component.actionbutton.ActionButton
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.common_retry
import org.jetbrains.compose.resources.stringResource

/**
 * Centered error message for the common "screen failed to load" state.
 *
 * When [onRetry] is non-null, a "Retry" [ActionButton] (label configurable via [retryLabel]) is
 * shown below the message. Screens that aren't refreshable (or that surface retry another way)
 * can leave [onRetry] null to render just the message.
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    onRetry: (() -> Unit)? = null,
    retryLabel: String = stringResource(Res.string.common_retry),
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (onRetry != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActionButton(text = retryLabel, onClick = onRetry)
            }
        } else {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
