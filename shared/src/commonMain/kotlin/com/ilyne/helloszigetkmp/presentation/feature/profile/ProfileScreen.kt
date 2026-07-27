package com.ilyne.helloszigetkmp.presentation.feature.profile

import androidx.compose.foundation.ScrollState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.ilyne.helloszigetkmp.presentation.component.status.ErrorState
import com.ilyne.helloszigetkmp.presentation.component.status.LoadingBox
import com.ilyne.helloszigetkmp.presentation.feature.LocalBottomBarPadding
import com.ilyne.helloszigetkmp.presentation.feature.contentBottomInset
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
import hello_sziget_kmp.shared.generated.resources.profile_days
import hello_sziget_kmp.shared.generated.resources.profile_error_accept_friend_request
import hello_sziget_kmp.shared.generated.resources.profile_error_decline_friend_request
import hello_sziget_kmp.shared.generated.resources.profile_error_load_profile
import hello_sziget_kmp.shared.generated.resources.profile_error_remove_friend
import hello_sziget_kmp.shared.generated.resources.profile_error_session_expired
import hello_sziget_kmp.shared.generated.resources.profile_error_upload_photo
import hello_sziget_kmp.shared.generated.resources.profile_friends
import hello_sziget_kmp.shared.generated.resources.profile_hearted
import hello_sziget_kmp.shared.generated.resources.profile_log_out_confirm
import hello_sziget_kmp.shared.generated.resources.profile_log_out_content_description
import hello_sziget_kmp.shared.generated.resources.profile_log_out_message
import hello_sziget_kmp.shared.generated.resources.profile_log_out_title
import hello_sziget_kmp.shared.generated.resources.profile_no_friends
import hello_sziget_kmp.shared.generated.resources.profile_remove_friend_confirm
import hello_sziget_kmp.shared.generated.resources.profile_remove_friend_message
import hello_sziget_kmp.shared.generated.resources.profile_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
    val currentOnNavigateToAddFriend by rememberUpdatedState(onNavigateToAddFriend)
    val currentOnLoggedOut by rememberUpdatedState(onLoggedOut)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateToAddFriend -> {
                    currentOnNavigateToAddFriend()
                }

                ProfileEffect.Logout -> {
                    logoutService.logout()
                    currentOnLoggedOut()
                }
            }
        }
    }

    PullToRefreshContent(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier,
    ) {
        // Loading/Error only replace the screen when there's no cached data to show yet (e.g.
        // the initial load, or a SESSION_EXPIRED error with no user loaded at all); once the
        // profile has loaded, a background failure keeps showing that data with a non-blocking
        // error banner instead of blanking the whole screen. See DiscoverScreen/ScheduleScreen
        // for the same pattern.
        val hasData = uiState.name != null
        val error = uiState.error
        when {
            uiState.isLoading && !hasData -> {
                LoadingBox(modifier = Modifier.fillMaxSize().testTag("profile_loading"))
            }

            error != null && !hasData -> {
                ErrorState(
                    message = stringResource(profileErrorMessageRes(error)),
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize().testTag("profile_error_state"),
                )
            }

            else -> {
                ProfileContent(uiState = uiState, viewModel = viewModel, launchImagePicker = launchImagePicker, vertScroll = vertScroll)
            }
        }
    }

    uiState.removeFriendAlert?.let { friend ->
        ConfirmationDialog(
            title = friend.name,
            message = stringResource(Res.string.profile_remove_friend_message, friend.name),
            confirmText = stringResource(Res.string.profile_remove_friend_confirm),
            onConfirm = { viewModel.onIntent(ProfileIntent.RemoveFriendClicked) },
            onDismiss = { viewModel.onIntent(ProfileIntent.DismissRemoveFriendAlert) },
        )
    }

    if (uiState.showLogoutAlert) {
        ConfirmationDialog(
            title = stringResource(Res.string.profile_log_out_title),
            message = stringResource(Res.string.profile_log_out_message),
            confirmText = stringResource(Res.string.profile_log_out_confirm),
            onConfirm = { viewModel.onIntent(ProfileIntent.ConfirmLogout) },
            onDismiss = { viewModel.onIntent(ProfileIntent.DismissLogoutAlert) },
        )
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    launchImagePicker: () -> Unit,
    vertScroll: ScrollState,
) {
    Column(
        modifier = Modifier
            .testTag("profile_screen_root")
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        MainHeader(
            text = stringResource(Res.string.profile_title),
            trailingContent = {
                IconButton(
                    onClick = { viewModel.onIntent(ProfileIntent.LogoutClicked) },
                    modifier = Modifier.testTag("profile_logout_button"),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_logout),
                        contentDescription = stringResource(Res.string.profile_log_out_content_description),
                        tint = AppTheme.colors.highlightYellow,
                    )
                }
            },
        )

        val error = uiState.error
        if (error != null) {
            ErrorState(
                message = stringResource(profileErrorMessageRes(error)),
                onDismiss = { viewModel.onIntent(ProfileIntent.DismissError) },
                modifier = Modifier.fillMaxWidth().testTag("profile_error_banner"),
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(state = vertScroll),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            ProfileAvatarAndName(
                modifier = Modifier.testTag("profile_avatar_and_name"),
                name = uiState.name.orEmpty(),
                avatarText = uiState.name.orEmpty().initials(),
                imageUrl = uiState.pendingImageUrl ?: uiState.imageUrl,
                isUploading = uiState.isUploadingImage,
                onAvatarClick = launchImagePicker,
            )
            Spacer(modifier = Modifier.height(24.dp))
            ProfileEngagementCountCard(
                modifier = Modifier.testTag("profile_engagement_count_card").fillMaxWidth(),
                items = listOf(
                    ProfileEngagementCountItemState(uiState.likedArtistCount, stringResource(Res.string.profile_hearted)),
                    ProfileEngagementCountItemState(uiState.friends.size, stringResource(Res.string.profile_friends)),
                    ProfileEngagementCountItemState(6, stringResource(Res.string.profile_days)),
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.profile_friends),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                ActionIconButton(
                    onClick = {
                        viewModel.onIntent(ProfileIntent.AddFriend)
                    },
                    modifier = Modifier.testTag("profile_add_friend_button"),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_person_add),
                        contentDescription = null,
                    )
                }
            }

            if (uiState.friends.isEmpty() && uiState.friendRequests.isEmpty()) {
                Text(
                    modifier = Modifier.testTag("profile_no_friends_message").fillMaxWidth().padding(top = 32.dp),
                    text = stringResource(Res.string.profile_no_friends),
                    textAlign = TextAlign.Center,
                    color = AppTheme.colors.navy,
                )
            }

            if (uiState.friendRequests.isNotEmpty()) {
                ProfileFriendRequestsSection(
                    modifier = Modifier.testTag("profile_friend_requests_section"),
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
                    modifier = Modifier.testTag("profile_friends_list"),
                    friends = uiState.friends,
                    onClick = {
                        viewModel.onIntent(ProfileIntent.ViewFriend(friendId = it))
                    },
                )
            }

            Spacer(modifier = Modifier.height(LocalBottomBarPadding.contentBottomInset))
        }
    }
}

private fun profileErrorMessageRes(reason: ProfileErrorReason) =
    when (reason) {
        ProfileErrorReason.SESSION_EXPIRED -> Res.string.profile_error_session_expired
        ProfileErrorReason.LOAD_PROFILE_FAILED -> Res.string.profile_error_load_profile
        ProfileErrorReason.UPLOAD_PHOTO_FAILED -> Res.string.profile_error_upload_photo
        ProfileErrorReason.ACCEPT_FRIEND_REQUEST_FAILED -> Res.string.profile_error_accept_friend_request
        ProfileErrorReason.DECLINE_FRIEND_REQUEST_FAILED -> Res.string.profile_error_decline_friend_request
        ProfileErrorReason.REMOVE_FRIEND_FAILED -> Res.string.profile_error_remove_friend
    }

@Composable
fun ProfileAvatarAndName(
    name: String,
    avatarText: String,
    imageUrl: String? = null,
    isUploading: Boolean = false,
    onAvatarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProfileAvatar(
                elevated = true,
                avatarText = avatarText,
                imageUrl = imageUrl,
                onClick = if (isUploading) null else onAvatarClick,
                modifier = Modifier.size(100.dp),
            )
            if (isUploading) {
                CircularProgressIndicator(color = AppTheme.colors.onAccent)
            }
        }
        Text(
            modifier = Modifier.testTag("profile_name"),
            text = name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    AppTheme {
        ProfileScreen()
    }
}
