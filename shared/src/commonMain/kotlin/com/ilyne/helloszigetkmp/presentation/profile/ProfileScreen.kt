package com.ilyne.helloszigetkmp.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.compose.MainHeader
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
                ProfileHeader(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "Requests - 2"
                )
                Column(
                    modifier = modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { count ->
                        ProfileFriendRequestItem(
                            item = ProfileFriendRequestItemState(name =" Zack Jones"),
                            onAccept = {},
                            onDecline = {}
                        )
                    }
                }
            }

            val hasFriends = true
            if (hasFriends) {
                ProfileHeader(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "Following - 2"
                )
                Column(
                    modifier = modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { count ->
                        ProfileFriendItem(
                            item = ProfileFriendItemState(name =" Zaira Tomayeva"),
                            onClick = {},
                        )
                    }
                }
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


@Composable
fun ProfileHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        text = text.uppercase(),
        textAlign = TextAlign.Start,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant

    )
}

@Preview
@Composable
fun ProfileScreenPreview() {
    AppTheme {
        ProfileScreen()
    }
}
