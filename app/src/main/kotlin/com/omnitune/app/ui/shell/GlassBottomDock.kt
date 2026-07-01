package com.omnitune.app.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes

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
            .padding(horizontal = 18.dp)
            .height(72.dp)
            .shadow(
                4.dp,
                OmniShapes.Dock,
                ambientColor = Color.Black.copy(alpha = 0.24f),
                spotColor = OmniColors.OmniAccentGlow.copy(alpha = 0.04f)
            )
            .clip(OmniShapes.Dock)
            .border(1.dp, OmniColors.SurfaceHairline.copy(alpha = 0.55f), OmniShapes.Dock)
            .background(
                Brush.verticalGradient(
                    listOf(OmniColors.SurfacePanel, OmniColors.OmniBackgroundBase.copy(alpha = 0.92f))
                )
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            
            val tint by animateColorAsState(
                targetValue = if (selected) OmniColors.Secondary else OmniColors.TextMuted,
                animationSpec = tween(durationMillis = 250),
                label = "color"
            )
            val iconSize by animateDpAsState(
                targetValue = if (selected) 24.dp else 22.dp,
                animationSpec = tween(durationMillis = 250),
                label = "size"
            )
            val backgroundAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(durationMillis = 250),
                label = "bg_alpha"
            )
            
            Box(
                modifier = Modifier
                    .weight(if (selected) 1.2f else 1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(
                        remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(bounded = true, color = OmniColors.OmniAccentSecondary.copy(alpha = 0.15f))
                    ) { onNavigate(item.route) }
                    .then(
                        if (backgroundAlpha > 0f) Modifier.background(
                Brush.horizontalGradient(
                    listOf(
                                    OmniColors.OmniAccentSecondary.copy(alpha = 0.10f * backgroundAlpha),
                                    OmniColors.OmniAccentPrimary.copy(alpha = 0.08f * backgroundAlpha)
                                )
                            )
                        ) else Modifier
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painterResource(item.resId),
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(iconSize)
                    )
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selected,
                        enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
                    ) {
                        Text(
                            item.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tint,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
