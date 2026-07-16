package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.presentation.component.header.ListSection
import com.ilyne.helloszigetkmp.util.text.initials

@Composable
fun ProfileFriendRequestsSection(
    friendRequests: List<User>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListSection(
        title = "Requests - ${friendRequests.size}",
        items = friendRequests,
        itemKey = { it.id },
        modifier = modifier,
    ) { friend ->
        ProfileFriendRequestItem(
            item = ProfileFriendRequestItemState(
                name = friend.name,
                avatarText = friend.name.initials(),
                imageUrl = friend.imageUrl,
            ),
            onAccept = { onAccept(friend.id) },
            onDecline = { onDecline(friend.id) },
        )
    }
}
