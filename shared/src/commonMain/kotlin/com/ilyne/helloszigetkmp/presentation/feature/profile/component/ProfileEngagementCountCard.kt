package com.ilyne.helloszigetkmp.presentation.feature.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme


@Composable
fun ProfileEngagementCountCard(
    items: List<ProfileEngagementCountItemState>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(IntrinsicSize.Min),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.tealGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ProfileEngagementCountItem(
                item = items[0],
                modifier = Modifier.weight(1f)
            )

            for (item in items.drop(1)) {
                VerticalDivider(modifier = Modifier.fillMaxHeight().background(color = AppTheme.colors.navy))
                ProfileEngagementCountItem(
                    item = item,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class ProfileEngagementCountItemState(
    val count: Int,
    val label: String
)

@Composable
fun ProfileEngagementCountItem(
    item: ProfileEngagementCountItemState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = item.count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppTheme.colors.highlightYellow
        )
        Text(
            text = item.label,
            fontSize = 12.sp,
            color = AppTheme.colors.onTealGreen,
            fontWeight = FontWeight.SemiBold
        )
    }
}
