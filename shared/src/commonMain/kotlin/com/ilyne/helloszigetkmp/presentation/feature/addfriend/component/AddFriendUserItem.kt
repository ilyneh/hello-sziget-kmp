package com.ilyne.helloszigetkmp.presentation.feature.addfriend.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.component.FriendListItem
import com.ilyne.helloszigetkmp.presentation.feature.addfriend.FriendshipStatus
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileAvatar
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme

@Composable
fun AddFriendUserItem(
    name: String,
    status: FriendshipStatus,
    onAdd: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FriendListItem(
        onClick = {},
        modifier = modifier
    ) {
        ProfileAvatar(
            avatarText = name
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(separator = "") { it.first().uppercase() },
            modifier = Modifier.width(48.dp).height(48.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = name,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        when (status) {
            FriendshipStatus.NONE -> AddFriendUserItemActionButton(text = "Request", onClick = onAdd)
            FriendshipStatus.REQUEST_RECEIVED -> AddFriendUserItemActionButton(
                text = "Accept",
                onClick = onAccept
            )
            FriendshipStatus.REQUEST_SENT -> AddFriendUserItemActionButton(
                text = "Requested",
                onClick = {},
                enabled = false
            )
            FriendshipStatus.FRIEND -> AddFriendUserItemActionButton(
                text = "Friends",
                onClick = {},
                enabled = false
            )
        }
    }
}

@Composable
fun AddFriendUserItemActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier,
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Preview
@Composable
fun AddFriendUserItemPreview() {
    AppTheme {
        AddFriendUserItem(
            name = "Zack Jones",
            status = FriendshipStatus.NONE,
            onAdd = {},
            onAccept = {},
        )
    }
}
