package com.omnitune.app.ui.shell

import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitune.app.R
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.LocalOmniColors
import com.omnitune.app.ui.theme.OmniGlassDefaults
import com.omnitune.app.ui.theme.OmniGlassSurface
import com.omnitune.app.ui.theme.OmniShapes

private data class NavItem(
    val iconRes: Int,
    val label: String,
    val route: String,
)

private val navItems = listOf(
    NavItem(R.drawable.ic_home, "Home", "home"),
    NavItem(R.drawable.ic_insights, "Stats", "stats"),
    NavItem(R.drawable.ic_history, "History", "history"),
    NavItem(R.drawable.ic_list, "Library", "library"),
)

@Composable
fun GlassBottomDock(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    pureBlack: Boolean = false,
) {
    // Header search retains Home selection; Downloads is a Library destination.
    val selectedIndex = when {
        currentRoute == "search" -> 0
        currentRoute == "downloads" -> navItems.indexOfFirst { it.route == "library" }
        else -> navItems.indexOfFirst { item ->
            currentRoute == item.route || currentRoute?.startsWith("${item.route}?") == true
        }.coerceAtLeast(0)
    }

    OmniGlassSurface(
        shape = OmniShapes.Dock,
        style = OmniGlassDefaults.navigationBarStyle(isPureBlack = pureBlack),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OmniChrome.BottomDockHorizontalPadding)
            .height(OmniChrome.BottomDockHeight),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
        ) {
            val tabWidth = maxWidth / navItems.size
            // The reference uses a shallow, wide coral capsule behind the selected icon.
            val pillWidth = 46.dp
            val pillHeight = 24.dp

            val indicatorOffset by animateDpAsState(
                targetValue = (tabWidth * selectedIndex) + ((tabWidth - pillWidth) / 2),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "nav_pill_slider",
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorOffset.roundToPx(), 6.dp.roundToPx()) }
                    .width(pillWidth)
                    .height(pillHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalOmniColors.current.accent.copy(alpha = 0.20f))
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navItems.forEachIndexed { index, item ->
                    NavTabItem(
                        item = item,
                        isSelected = selectedIndex == index,
                        onNavigate = { onNavigate(item.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavTabItem(
    item: NavItem,
    isSelected: Boolean,
    onNavigate: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val tint by animateColorAsState(
        targetValue = if (isSelected) LocalOmniColors.current.accent else LocalOmniColors.current.textTertiary,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nav_tint_${item.route}",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onNavigate,
            )
            .semantics {
                role = Role.Tab
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
