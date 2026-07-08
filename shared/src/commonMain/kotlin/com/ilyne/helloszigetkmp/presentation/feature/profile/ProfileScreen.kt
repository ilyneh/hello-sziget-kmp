package com.ilyne.helloszigetkmp.presentation.feature.profile

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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.presentation.component.MainHeader
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileAvatar
import com.ilyne.helloszigetkmp.presentation.profile.components.ProfileEngagementCountCard
import com.ilyne.helloszigetkmp.presentation.profile.components.ProfileEngagementCountItemState
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileFriendRequestsSection
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileFriendsSection
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_person_add
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, onNavigateToAddFriend: () -> Unit = {}) {
    val viewModel = koinViewModel<ProfileViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vertScroll = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToAddFriend -> onNavigateToAddFriend()
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
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
            ProfileAvatarAndName(
                name = "Ilyne",
                avatarText = "IH",
            )
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
                        viewModel.onIntent(ProfileIntent.AddFriend)
                    },
                    shape = IconButtonDefaults.mediumSquareShape
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_person_add),
                        contentDescription = null
                    )
                }
            }

            if (uiState.friendRequests.isNotEmpty()) {
                ProfileFriendRequestsSection(
                    friendRequests = uiState.friendRequests,
                    onAccept = {
                        viewModel.onIntent(ProfileIntent.AcceptFriendRequest(friendId = it))
                    },
                    onDecline = {
                        viewModel.onIntent(ProfileIntent.DeclineFriendRequest(friendId = it))
                    },
                )
            }

            if (uiState.friends.isNotEmpty()) {
                ProfileFriendsSection(
                    friends = uiState.friends,
                    onClick = {
                        viewModel.onIntent(ProfileIntent.ViewFriend(friendId = it))
                    }
                )
            }
        }

    }
}

@Composable
fun ProfileAvatarAndName(
    name: String,
    avatarText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileAvatar(
            elevated = true,
            avatarText = avatarText,
            modifier = Modifier.width(72.dp).height(72.dp)
        )
        Text(
            text = name,
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
