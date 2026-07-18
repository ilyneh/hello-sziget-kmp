package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendListItem
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.NamedAvatarRow
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_carat_right
import hello_sziget_kmp.shared.generated.resources.profile_view_friend_content_description
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class ProfileFriendItemState(
    val name: String,
    val avatarText: String,
    val imageUrl: String? = null,
)

@Composable
fun ProfileFriendItem(
    item: ProfileFriendItemState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FriendListItem(
        onClick = onClick,
        modifier = modifier,
    ) {
        NamedAvatarRow(
            avatarText = item.avatarText,
            imageUrl = item.imageUrl,
            name = item.name,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_carat_right),
                contentDescription = stringResource(Res.string.profile_view_friend_content_description),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Preview
@Composable
private fun ProfileFriendItemPreview() {
    AppTheme {
        ProfileFriendItem(
            item = ProfileFriendItemState(
                name = "Zack Jones",
                avatarText = "ZJ",
            ),
            onClick = {},
        )
    }
}
