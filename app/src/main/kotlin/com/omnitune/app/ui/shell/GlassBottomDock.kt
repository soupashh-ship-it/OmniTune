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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitune.app.R
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniColors
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

/**
 * Fluid bottom navigation dock with a spring-animated sliding pill indicator.
 *
 * Architecture:
 * ```
 * OmniGlassSurface (shadow + blurred background + border)
 *   └── BoxWithConstraints
 *        ├── Pill indicator (spring-animated x-offset)
 *        └── Row of clickable nav items
 * ```
 *
 * The pill smoothly interpolates between tab positions using a low-stiffness
 * spring, giving the fluid feel. Each item also has per‑tab micro‑animations
 * (icon scale, tint).
 */
@Composable
fun GlassBottomDock(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    OmniGlassSurface(
        shape = OmniShapes.Dock,
        style = OmniGlassDefaults.NavigationBarDark,
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
            // Pill is roughly centered on the icon column area of each tab
            val pillWidth = 40.dp
            val pillHeight = 28.dp

            // ── Spring-animated pill offset ──────────────────────────────
            val indicatorOffset by animateDpAsState(
                targetValue = (tabWidth * selectedIndex) + ((tabWidth - pillWidth) / 2),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "nav_pill_slider",
            )

            // ── Pill indicator (behind items) ─────────────────────────────
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset, y = 10.dp)
                    .width(pillWidth)
                    .height(pillHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(OmniColors.OmniAccentSecondary.copy(alpha = 0.15f)),
            )

            // ── Nav items (sharp, interactive) ────────────────────────────
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

    // Per‑tab micro‑animations
    val tint by animateColorAsState(
        targetValue = if (isSelected) OmniColors.OmniAccentSecondary else OmniColors.TextMuted,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nav_tint_${item.route}",
    )
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 26.dp else 23.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "nav_icon_size_${item.route}",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onNavigate,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(3.dp))

        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = item.label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = tint.copy(alpha = if (isSelected) 1f else 0.60f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
