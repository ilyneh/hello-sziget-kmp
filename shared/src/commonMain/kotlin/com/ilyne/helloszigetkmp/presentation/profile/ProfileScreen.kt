package com.ilyne.helloszigetkmp.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.compose.MainHeader
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MainHeader(
                text = "Profile",
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(32.dp))

            ProfileAvatarAndName()

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ProfileMyEngagementCount(
                        modifier = Modifier.weight(1f),
                        count = 16,
                        label = "Liked"
                    )

                    VerticalDivider(modifier = Modifier.fillMaxHeight())

                    ProfileMyEngagementCount(
                        modifier = Modifier.weight(1f),
                        count = 11,
                        label = "Friends"
                    )

                    VerticalDivider(modifier = Modifier.fillMaxHeight())

                    ProfileMyEngagementCount(
                        modifier = Modifier.weight(1f),
                        count = 6,
                        label = "Days"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Row(
                    modifier = modifier.fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Friends",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "See all ->",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer

                    )
                }

                val hasFriendRequests = true
                if (hasFriendRequests) {
                    Box(
                        modifier = modifier.fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(color = MaterialTheme.colorScheme.primaryContainer)
                            .border(width = .5.dp, color = MaterialTheme.colorScheme.onPrimaryContainer, shape = RoundedCornerShape(16.dp)),
                    ) {
                        Row(modifier = Modifier.padding(8.dp)
                            .padding(start = 8.dp, end = 8.dp)
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "2 Friend Requests",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ">",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer

                            )
                        }


                    }
                }

            }
            // TODO: user avatar, display name, email
            // TODO: share lineup button (native share sheet via expect/actual)
            // TODO: friends list
            // TODO: sign out
        }
    }
}

@Composable
fun ProfileMyEngagementCount(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun ProfileAvatarAndName(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileAvatar(modifier = Modifier.width(72.dp).height(72.dp))
        Text(
            text = "Ilyne",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ProfileAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Green)
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center).padding(4.dp),
            text = "IH",
            color = Color.White,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 16.sp,        // Minimum allowable size
                maxFontSize = 80.sp,        // Maximum allowable size
                stepSize = 1.sp             // Granularity of adjustment
            ),
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

@Preview
@Composable
fun ProfileAvatarAndNamePreview() {
    Box(Modifier.background(Color.White).padding(24.dp)) {
        ProfileAvatarAndName()
    }
}
