package com.ilyne.helloszigetkmp.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(32.dp))
        // TODO: user avatar, display name, email
        // TODO: share lineup button (native share sheet via expect/actual)
        // TODO: friends list
        // TODO: sign out
        Text("Profile screen — coming soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
