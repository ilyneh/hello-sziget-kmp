package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendListItem
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_carat_right
import org.jetbrains.compose.resources.painterResource


data class ProfileFriendItemState(
    val name: String,
    val avatarText: String,
    val imageUrl: String? = null,
)

@Composable
fun ProfileFriendItem(
    item: ProfileFriendItemState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FriendListItem(
        onClick = onClick,
        modifier = modifier,
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

        Icon(
            painter = painterResource(Res.drawable.ic_carat_right),
            contentDescription = "View Friend",
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Preview
@Composable
fun ProfileFriendItemPreview() {
    AppTheme {
        ProfileFriendItem(
            item = ProfileFriendItemState(
                name = "Zack Jones",
                avatarText = "ZJ"
            ),
            onClick = {},
        )
    }
}
