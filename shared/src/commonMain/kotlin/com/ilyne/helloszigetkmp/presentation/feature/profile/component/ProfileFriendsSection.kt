package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.presentation.component.SectionHeader

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
        friends.forEach { friend ->
            ProfileFriendItem(
                item = ProfileFriendItemState(
                    name = friend.name,
                    avatarText = friend.name
                        .split(" ")
                        .joinToString(separator = "") { it.first().uppercase() }
                ),
                onClick = { onClick(friend.id) },
            )
        }
    }
}
