package com.omnitune.app.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitune.app.R
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniMotion
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.omniPressScaleBounce

@Composable
fun GlassBottomDock(currentRoute: String?, onNavigate: (String) -> Unit) {
    data class NavItem(val resId: Int, val label: String, val route: String)
    val navItems = listOf(
        NavItem(R.drawable.ic_home, "Home", "home"),
        NavItem(R.drawable.ic_insights, "Stats", "stats"),
        NavItem(R.drawable.ic_history, "History", "history"),
        NavItem(R.drawable.ic_list, "Library", "library"),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OmniChrome.BottomDockHorizontalPadding)
            .height(OmniChrome.BottomDockHeight)
            .shadow(
                6.dp,
                OmniShapes.Dock,
                ambientColor = Color.Black.copy(alpha = 0.32f),
                spotColor = OmniColors.OmniAccentGlow.copy(alpha = 0.06f)
            )
            .clip(OmniShapes.Dock)
            .background(OmniColors.OmniGlassDock)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route

            val tint by animateColorAsState(
                targetValue = if (selected) OmniColors.Secondary else OmniColors.TextMuted,
                animationSpec = OmniMotion.gentleSpring(),
                label = "nav_color_${item.route}"
            )
            val iconSize by animateDpAsState(
                targetValue = if (selected) 23.dp else 20.dp,
                animationSpec = OmniMotion.pressSpring(),
                label = "nav_icon_${item.route}"
            )
            val pillAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = OmniMotion.gentleSpring(),
                label = "nav_pill_${item.route}"
            )
            val navItemInteraction = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .omniPressScaleBounce(navItemInteraction)
                    .clickable(
                        navItemInteraction,
                        indication = androidx.compose.material3.ripple(
                            bounded = true,
                            color = OmniColors.OmniAccentSecondary.copy(alpha = 0.12f)
                        )
                    ) { onNavigate(item.route) }
                    .then(
                        if (pillAlpha > 0f) Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    OmniColors.OmniAccentSecondary.copy(alpha = 0.18f * pillAlpha),
                                    OmniColors.OmniAccentPrimary.copy(alpha = 0.12f * pillAlpha)
                                )
                            )
                        ) else Modifier
                    )
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        painterResource(item.resId),
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(iconSize)
                    )
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = tint.copy(alpha = if (selected) 1f else 0.60f),
                    )
                }
            }
        }
    }
}
