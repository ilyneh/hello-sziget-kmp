package com.ilyne.helloszigetkmp.presentation.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ilyne.helloszigetkmp.core.auth.LogoutService
import com.ilyne.helloszigetkmp.presentation.component.actionbutton.ActionIconButton
import com.ilyne.helloszigetkmp.presentation.component.header.MainHeader
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileAvatar
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileEngagementCountCard
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileEngagementCountItemState
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileFriendRequestsSection
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileFriendsSection
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.presentation.theme.SzigetPalette
import hello_sziget_kmp.shared.generated.resources.Res
import hello_sziget_kmp.shared.generated.resources.ic_logout
import hello_sziget_kmp.shared.generated.resources.ic_person_add
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateToAddFriend: () -> Unit = {},
    onNavigateToPhotoPicker: () -> Unit = {},
    pickedPhotoUrl: String? = null,
    onPickedPhotoConsumed: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
) {
    val logoutService = koinInject<LogoutService>()
    val viewModel = koinViewModel<ProfileViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vertScroll = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToAddFriend -> onNavigateToAddFriend()
                ProfileEffect.NavigateToPhotoPicker -> onNavigateToPhotoPicker()
                ProfileEffect.Logout -> {
                    logoutService.logout()
                    onLoggedOut()
                }
            }
        }
    }

    LaunchedEffect(pickedPhotoUrl) {
        if (pickedPhotoUrl != null) {
            viewModel.onPhotoPicked(pickedPhotoUrl)
            onPickedPhotoConsumed()
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = modifier.fillMaxSize()
                .verticalScroll(state = vertScroll)
                .background(color = SzigetPalette.Peach)
        ) {
            MainHeader(
                text = "Profile",
                trailingContent = {
                    IconButton(
                        onClick = { viewModel.onIntent(ProfileIntent.LogoutClicked) }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_logout),
                            contentDescription = "Log out",
                            tint = SzigetPalette.SunshineYellow,
                        )
                    }
                }
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                ProfileAvatarAndName(
                    name = uiState.name.orEmpty(),
                    avatarText = uiState.name.orEmpty()
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .joinToString(separator = ""),
                    imageUrl = uiState.picture,
                    onAvatarClick = { viewModel.onIntent(ProfileIntent.AvatarClicked) },
                )
                Spacer(modifier = Modifier.height(24.dp))
                ProfileEngagementCountCard(
                    modifier = Modifier.fillMaxWidth(),
                    items = listOf(
                        ProfileEngagementCountItemState(uiState.likedArtistCount, "Hearted"),
                        ProfileEngagementCountItemState(uiState.friends.size, "Friends"),
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

                    ActionIconButton(
                        onClick = {
                            viewModel.onIntent(ProfileIntent.AddFriend)
                        },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_person_add),
                            contentDescription = null
                        )
                    }
                }

                if (uiState.friends.isEmpty() && uiState.friendRequests.isEmpty()) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        text = "No friends",
                        textAlign = TextAlign.Center,
                        color = SzigetPalette.Navy,
                    )
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

    uiState.removeFriendAlert?.let { friend ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ProfileIntent.DismissRemoveFriendAlert) },
            title = { Text(text = friend.name) },
            text = { Text(text = "Remove ${friend.name} as a friend?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onIntent(ProfileIntent.RemoveFriendClicked) }
                ) {
                    Text(
                        text = "Remove friend",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onIntent(ProfileIntent.DismissRemoveFriendAlert) }
                ) {
                    Text(text = "Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (uiState.showLogoutAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(ProfileIntent.DismissLogoutAlert) },
            title = { Text(text = "Log out?") },
            text = { Text(text = "Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onIntent(ProfileIntent.ConfirmLogout) }
                ) {
                    Text(
                        text = "Log out",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onIntent(ProfileIntent.DismissLogoutAlert) }
                ) {
                    Text(text = "Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun ProfileAvatarAndName(
    name: String,
    avatarText: String,
    imageUrl: String? = null,
    onAvatarClick: (() -> Unit)? = null,
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
            imageUrl = imageUrl,
            onClick = onAvatarClick,
            modifier = Modifier.size(100.dp)
        )
        Text(
            text = name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SzigetPalette.PrimaryBlue
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
