package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.domain.model.User

@Composable
fun ProfileFriendRequestsSection(
    friendRequests: List<User>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ProfileSectionHeader(
        modifier = modifier.padding(top = 8.dp),
        text = "Requests - ${friendRequests.size}"
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        friendRequests.forEach { friend ->
            ProfileFriendRequestItem(
                item = ProfileFriendRequestItemState(
                    name = friend.name,
                    avatarText = friend.name
                        .split(" ")
                        .joinToString(separator = "") { it.first().uppercase() }
                ),
                onAccept = { onAccept(friend.id) },
                onDecline = { onDecline(friend.id) }
            )
        }
    }
}
