package com.ilyne.helloszigetkmp.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyne.helloszigetkmp.compose.MainHeader

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MainHeader(
            text = "Profile",
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(32.dp))
        // TODO: user avatar, display name, email
        // TODO: share lineup button (native share sheet via expect/actual)
        // TODO: friends list
        // TODO: sign out
        Text("Profile screen — coming soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
