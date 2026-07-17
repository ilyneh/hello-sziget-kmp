package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.presentation.component.header.ListSection
import com.ilyne.helloszigetkmp.util.text.initials

@Composable
fun ProfileFriendsSection(
    friends: List<User>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListSection(
        title = "Following - ${friends.size}",
        items = friends,
        itemKey = { it.id },
        modifier = modifier,
    ) { friend ->
        ProfileFriendItem(
            item = ProfileFriendItemState(
                name = friend.name,
                avatarText = friend.name.initials(),
                imageUrl = friend.imageUrl,
            ),
            onClick = { onClick(friend.id) },
        )
    }
}
