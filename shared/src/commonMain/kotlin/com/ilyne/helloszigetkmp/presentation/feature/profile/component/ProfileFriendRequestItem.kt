package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.ilyne.helloszigetkmp.presentation.component.actionbutton.ActionIconButton
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendListItem
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.NamedAvatarRow
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_cancel
import hello_sziget_kmp.shared.generated.resources.ic_check
import hello_sziget_kmp.shared.generated.resources.profile_accept_friend_request_content_description
import hello_sziget_kmp.shared.generated.resources.profile_decline_friend_request_content_description
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
    modifier: Modifier = Modifier,
) {
    FriendListItem(
        onClick = {},
        modifier = modifier.testTag("profile_friend_request_item_${item.name}"),
        backgroundColor = MaterialTheme.colorScheme.surface,
    ) {
        NamedAvatarRow(
            avatarText = item.avatarText,
            imageUrl = item.imageUrl,
            name = item.name,
        ) {
            ActionIconButton(
                modifier = Modifier.testTag("profile_accept_friend_request_button_${item.name}"),
                onClick = onAccept,
                shape = IconButtonDefaults.mediumSquareShape,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = stringResource(Res.string.profile_accept_friend_request_content_description),
                )
            }
            FilledIconButton(
                modifier = Modifier.testTag("profile_decline_friend_request_button_${item.name}"),
                onClick = onDecline,
                shape = IconButtonDefaults.mediumSquareShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.outline,
                ),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_cancel),
                    contentDescription = stringResource(Res.string.profile_decline_friend_request_content_description),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProfileFriendRequestItemPreview() {
    AppTheme {
        ProfileFriendRequestItem(
            item = ProfileFriendRequestItemState(
                name = "Zaira Tomayeva",
                avatarText = "ZT",
            ),
            onAccept = {},
            onDecline = {},
        )
    }
}
