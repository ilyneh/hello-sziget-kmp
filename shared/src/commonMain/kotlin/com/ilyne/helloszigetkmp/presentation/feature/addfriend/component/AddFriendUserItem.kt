package com.ilyne.helloszigetkmp.presentation.feature.addfriend.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.component.actionbutton.ActionButton
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendListItem
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.NamedAvatarRow
import com.ilyne.helloszigetkmp.presentation.feature.addfriend.FriendshipStatus
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.util.text.initials

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
        NamedAvatarRow(
            avatarText = name.initials(),
            imageUrl = null,
            name = name,
        ) {
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
}

@Composable
fun AddFriendUserItemActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ActionButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1
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
