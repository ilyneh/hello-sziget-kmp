package com.ilyne.helloszigetkmp.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.compose.MainHeader
import com.ilyne.helloszigetkmp.presentation.profile.components.ProfileAvatar
import com.ilyne.helloszigetkmp.presentation.profile.components.ProfileEngagementCountCard
import com.ilyne.helloszigetkmp.presentation.profile.components.ProfileEngagementCountItemState
import com.ilyne.helloszigetkmp.presentation.profile.components.ProfileFriendRequestsSection
import com.ilyne.helloszigetkmp.presentation.profile.components.ProfileFriendsSection
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_person_add
import org.jetbrains.compose.resources.painterResource

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val vertScroll = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = modifier.fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(vertScroll),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MainHeader(
                text = "Profile",
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(32.dp))
            ProfileAvatarAndName()
            Spacer(modifier = Modifier.height(32.dp))
            ProfileEngagementCountCard(
                modifier = Modifier.fillMaxWidth(),
                items = listOf(
                    ProfileEngagementCountItemState(16, "Hearted"),
                    ProfileEngagementCountItemState(11, "Friends"),
                    ProfileEngagementCountItemState(6, "Days"),
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Friends",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                FilledIconButton(
                    onClick = {
                        // TODO -
                    },
                    shape = IconButtonDefaults.mediumSquareShape
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_person_add),
                        contentDescription = null
                    )
                }
            }
            val hasFriendRequests = true
            if (hasFriendRequests) {
                ProfileFriendRequestsSection()
            }
            val hasFriends = true
            if (hasFriends) {
                ProfileFriendsSection()
            }
        }
    }
}

@Composable
fun ProfileAvatarAndName(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileAvatar(elevated = true, modifier = Modifier.width(72.dp).height(72.dp))
        Text(
            text = "Ilyne",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    AppTheme {
        ProfileScreen()
    }
}
