package com.ilyne.helloszigetkmp.presentation.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ilyne.helloszigetkmp.core.media.rememberProfileImagePicker
import com.ilyne.helloszigetkmp.presentation.component.actionbutton.ActionIconButton
import com.ilyne.helloszigetkmp.presentation.component.dialog.ConfirmationDialog
import com.ilyne.helloszigetkmp.presentation.component.header.MainHeader
import com.ilyne.helloszigetkmp.presentation.component.pulltorefresh.PullToRefreshContent
import com.ilyne.helloszigetkmp.presentation.feature.LocalBottomBarPadding
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileAvatar
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileEngagementCountCard
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileEngagementCountItemState
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileFriendRequestsSection
import com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileFriendsSection
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import com.ilyne.helloszigetkmp.util.text.initials
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
    onLoggedOut: () -> Unit = {},
) {
    val logoutService = koinInject<LogoutService>()
    val viewModel = koinViewModel<ProfileViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vertScroll = rememberScrollState()
    val launchImagePicker = rememberProfileImagePicker { image ->
        viewModel.onIntent(ProfileIntent.PhotoPicked(image))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToAddFriend -> onNavigateToAddFriend()
                ProfileEffect.Logout -> {
                    logoutService.logout()
                    onLoggedOut()
                }
            }
        }
    }

    PullToRefreshContent(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier
    ) {
        Column(
            modifier = modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
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
                            tint = AppTheme.colors.highlightYellow,
                        )
                    }
                }
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
                    .verticalScroll(state = vertScroll),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                ProfileAvatarAndName(
                    name = uiState.name.orEmpty(),
                    avatarText = uiState.name.orEmpty().initials(),
                    imageUrl = uiState.pendingImageUrl ?: uiState.imageUrl,
                    isUploading = uiState.isUploadingImage,
                    onAvatarClick = launchImagePicker,
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
                        color = AppTheme.colors.navy,
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

                Spacer(modifier = Modifier.height(LocalBottomBarPadding.current + 48.dp))
            }
        }
    }

    uiState.removeFriendAlert?.let { friend ->
        ConfirmationDialog(
            title = friend.name,
            message = "Remove ${friend.name} as a friend?",
            confirmText = "Remove friend",
            onConfirm = { viewModel.onIntent(ProfileIntent.RemoveFriendClicked) },
            onDismiss = { viewModel.onIntent(ProfileIntent.DismissRemoveFriendAlert) },
        )
    }

    if (uiState.showLogoutAlert) {
        ConfirmationDialog(
            title = "Log out?",
            message = "Are you sure you want to log out?",
            confirmText = "Log out",
            onConfirm = { viewModel.onIntent(ProfileIntent.ConfirmLogout) },
            onDismiss = { viewModel.onIntent(ProfileIntent.DismissLogoutAlert) },
        )
    }
}

@Composable
fun ProfileAvatarAndName(
    name: String,
    avatarText: String,
    imageUrl: String? = null,
    isUploading: Boolean = false,
    onAvatarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProfileAvatar(
                elevated = true,
                avatarText = avatarText,
                imageUrl = imageUrl,
                onClick = if (isUploading) null else onAvatarClick,
                modifier = Modifier.size(100.dp)
            )
            if (isUploading) {
                CircularProgressIndicator(color = AppTheme.colors.onAccent)
            }
        }
        Text(
            text = name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
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
