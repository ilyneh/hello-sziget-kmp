package com.ilyne.helloszigetkmp.presentation.feature.login.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.component.actionbutton.ActionButton
import com.ilyne.helloszigetkmp.presentation.component.header.ModalHeader
import com.ilyne.helloszigetkmp.presentation.component.sheet.AppModalBottomSheet
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.common_save
import hello_sziget_kmp.shared.generated.resources.login_debug_base_url_label
import hello_sziget_kmp.shared.generated.resources.login_debug_base_url_placeholder
import hello_sziget_kmp.shared.generated.resources.login_debug_sheet_title
import hello_sziget_kmp.shared.generated.resources.login_debug_skip_google_sign_in_label
import hello_sziget_kmp.shared.generated.resources.login_debug_token_label
import org.jetbrains.compose.resources.stringResource

/**
 * Debug-only bottom sheet (see [com.ilyne.helloszigetkmp.presentation.feature.login.LoginScreen]'s
 * "DEBUG" button, only shown when `LoginUiState.isDebugBuild` is true) letting a developer point
 * the app at a custom backend/token at runtime instead of rebuilding with
 * `-Psziget.localBackendUrl=...`/`-Psziget.localBearerToken=...` baked in - see
 * [com.ilyne.helloszigetkmp.core.config.DebugConfigStore].
 */
@Composable
fun DebugConfigSheet(
    baseUrl: String,
    token: String,
    skipGoogleSignIn: Boolean,
    onBaseUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSkipGoogleSignInChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppModalBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("login_debug_config_sheet"),
        ) {
            ModalHeader(text = stringResource(Res.string.login_debug_sheet_title))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text(stringResource(Res.string.login_debug_base_url_label)) },
                placeholder = { Text(stringResource(Res.string.login_debug_base_url_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("login_debug_base_url_input"),
            )

            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                label = { Text(stringResource(Res.string.login_debug_token_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("login_debug_token_input"),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.login_debug_skip_google_sign_in_label),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = skipGoogleSignIn,
                    onCheckedChange = onSkipGoogleSignInChange,
                    // testTag lives on the Switch itself, not the Row - a tap on the row's id
                    // would land on the label's bounds (no click handler of its own) rather than
                    // the Switch, same rationale as ScheduleFilterScreen's FilterSwitchRow.
                    modifier = Modifier.testTag("login_debug_skip_google_sign_in_switch"),
                )
            }

            ActionButton(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("login_debug_save_button"),
            ) {
                Text(
                    text = stringResource(Res.string.common_save),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Preview
@Composable
private fun DebugConfigSheetPreview() {
    AppTheme {
        DebugConfigSheet(
            baseUrl = "http://10.0.2.2:8081/api/v1",
            token = "",
            skipGoogleSignIn = true,
            onBaseUrlChange = {},
            onTokenChange = {},
            onSkipGoogleSignInChange = {},
            onSave = {},
            onDismiss = {},
        )
    }
}
