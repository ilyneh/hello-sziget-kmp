package com.ilyne.helloszigetkmp.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileFriendsSection(modifier: Modifier = Modifier) {
    ProfileSectionHeader(
        modifier = Modifier.padding(top = 8.dp),
        text = "Following - 2"
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { count ->
            ProfileFriendItem(
                item = ProfileFriendItemState(name = " Zaira Tomayeva"),
                onClick = {},
            )
        }
    }
}
