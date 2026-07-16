package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.component.actionbutton.ActionIconButton
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendListItem
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_cancel
import hello_sziget_kmp.shared.generated.resources.ic_check
import org.jetbrains.compose.resources.painterResource


data class ProfileFriendRequestItemState(
    val name: String,
    val avatarText: String,
    val imageUrl: String? = null,
)

@Composable
fun ProfileFriendRequestItem(
    item: ProfileFriendRequestItemState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    FriendListItem(
        onClick = {},
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surface,
    ) {
        ProfileAvatar(
            avatarText = item.avatarText,
            imageUrl = item.imageUrl,
            modifier = Modifier.width(48.dp).height(48.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = item.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        ActionIconButton(
            onClick = onAccept,
            shape = IconButtonDefaults.mediumSquareShape
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_check),
                contentDescription = "Accept Friend Request"
            )
        }
        FilledIconButton(
            onClick = onDecline,
            shape = IconButtonDefaults.mediumSquareShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.outline
            )
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_cancel),
                contentDescription = "Decline Friend Request"
            )
        }
    }
}

@Preview
@Composable
fun ProfileFriendRequestItemPreview() {
    AppTheme {
        ProfileFriendRequestItem(
            item = ProfileFriendRequestItemState(
                name = "Zaira Tomayeva",
                avatarText = "ZT"
            ),
            onAccept = {},
            onDecline = {}
        )
    }
}
