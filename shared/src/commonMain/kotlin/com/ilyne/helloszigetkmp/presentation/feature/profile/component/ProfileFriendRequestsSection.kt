package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.presentation.component.header.SectionHeader

@Composable
fun ProfileFriendRequestsSection(
    friendRequests: List<User>,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionHeader(
        modifier = Modifier.padding(top = 8.dp),
        text = "Requests - ${friendRequests.size}"
    )
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val currentOnAccept by rememberUpdatedState(onAccept)
        val currentOnDecline by rememberUpdatedState(onDecline)
        friendRequests.forEach { friend ->
            key(friend.id) {
                // Stable per-row callbacks: see ProfileFriendsSection for why this matters
                // for avoiding avatar flashes on unrelated recompositions (e.g. refresh).
                val stableOnAccept = remember(friend.id) { { currentOnAccept(friend.id) } }
                val stableOnDecline = remember(friend.id) { { currentOnDecline(friend.id) } }
                ProfileFriendRequestItem(
                    item = ProfileFriendRequestItemState(
                        name = friend.name,
                        avatarText = friend.name
                            .split(" ")
                            .joinToString(separator = "") { it.first().uppercase() },
                        imageUrl = friend.imageUrl,
                    ),
                    onAccept = stableOnAccept,
                    onDecline = stableOnDecline
                )
            }
        }
    }
}
