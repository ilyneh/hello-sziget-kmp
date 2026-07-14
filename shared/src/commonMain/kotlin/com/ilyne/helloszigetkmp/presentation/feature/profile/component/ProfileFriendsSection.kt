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
fun ProfileFriendsSection(
    friends: List<User>,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionHeader(
        modifier = Modifier.padding(top = 8.dp),
        text = "Following - ${friends.size}"
    )
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val currentOnClick by rememberUpdatedState(onClick)
        friends.forEach { friend ->
            key(friend.id) {
                // Stable per-row callback: `onClick` (and the List itself) are new
                // instances on every recomposition of the caller, which would otherwise
                // force ProfileFriendItem -> ProfileAvatar -> AsyncImage to recompose
                // and briefly flash its unloaded state even when this friend's data
                // hasn't actually changed (e.g. every pull-to-refresh isLoading toggle).
                val stableOnClick = remember(friend.id) { { currentOnClick(friend.id) } }
                ProfileFriendItem(
                    item = ProfileFriendItemState(
                        name = friend.name,
                        avatarText = friend.name
                            .split(" ")
                            .joinToString(separator = "") { it.first().uppercase() },
                        imageUrl = friend.imageUrl,
                    ),
                    onClick = stableOnClick,
                )
            }
        }
    }
}
