package com.ilyne.helloszigetkmp.presentation.component.friendavatarstack

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileAvatar

/**
 * Shared "avatar + name" opening block used by the various [FriendListItem] rows
 * (add-friend, profile friends list, profile friend requests) before each row's
 * item-specific trailing content (action button, chevron, accept/decline icons, etc.).
 */
@Composable
fun RowScope.NamedAvatarRow(
    avatarText: String,
    imageUrl: String?,
    name: String,
    trailing: @Composable RowScope.() -> Unit,
) {
    ProfileAvatar(
        avatarText = avatarText,
        imageUrl = imageUrl,
        modifier = Modifier.width(48.dp).height(48.dp)
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
        modifier = Modifier.weight(1f),
        text = name,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
    trailing()
}
