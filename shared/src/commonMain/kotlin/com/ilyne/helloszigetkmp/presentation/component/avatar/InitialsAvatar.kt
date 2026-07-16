package com.ilyne.helloszigetkmp.presentation.component.avatar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Generic avatar shell shared by [com.ilyne.helloszigetkmp.presentation.feature.profile.component.ProfileAvatar]
 * and [com.ilyne.helloszigetkmp.presentation.component.friendavatarstack.FriendAvatarStack]'s private
 * `Avatar`: renders [imageUrl] cropped to fill when present, otherwise defers to [content] (typically
 * initials text). Shape, sizing, background, border, shadow, and click handling are all left to the
 * caller so each call site can preserve its own exact visual styling.
 */
@Composable
fun InitialsAvatar(
    imageUrl: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    border: BorderStroke? = null,
    shadowElevation: Dp = 0.dp,
    contentPadding: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .let { if (shadowElevation > 0.dp) it.shadow(elevation = shadowElevation, shape = shape) else it }
            .padding(contentPadding)
            .clip(shape)
            .background(backgroundColor)
            .let { if (border != null) it.border(border, shape) else it }
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            content()
        }
    }
}
