package com.ilyne.helloszigetkmp.presentation.component.search

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_discover
import org.jetbrains.compose.resources.painterResource

@Composable
fun SearchTextField(
    value: String,
    placeHolderText: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeHolderText) },
        leadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_discover),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(size = 16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedTextColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = AppTheme.colors.fieldBorder,
            focusedBorderColor = AppTheme.colors.highlightMagenta,
        ),
    )
}

@Preview
@Composable
private fun SearchTextFieldPreview() {
    AppTheme {
        SearchTextField(
            value = "Skirll",
            placeHolderText = "Search friends...",
            onValueChange = {},
        )

        SearchTextField(
            value = "Skirll",
            placeHolderText = "Search friends...",
            onValueChange = {},
        )
    }
}
