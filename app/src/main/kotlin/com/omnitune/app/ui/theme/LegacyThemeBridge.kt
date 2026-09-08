package com.omnitune.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Temporary bridge providing backward compatibility for unmigrated screens
 * while routing all presentation to SuvMusic 2.6.6.0 design tokens.
 */
object OmniColors {
    var OmniBackgroundBase: Color = Color.Black
    var OmniBackgroundElevated: Color = SurfaceContainerHigh
    var OmniBackgroundTop: Color = Color.Black
    var OmniBackgroundGradientTop: Brush = Brush.verticalGradient(listOf(Color.Black, Color.Black))
    var OmniAccentGlow: Color = Purple50.copy(alpha = 0.30f)
    var Background: Color = Color.Black
    var BackgroundGradient: Brush = Brush.verticalGradient(listOf(Color.Black, Color.Black))
    var SurfaceRaised: Color = SurfaceContainerHigh
    var SurfaceFloating: Color = SurfaceContainerHighest
    var SurfacePanel: Color = SurfaceContainer
    var SurfaceSubtle: Color = SurfaceContainerLowest
    var SurfaceQuiet: Color = SurfaceContainerLowest
    var Surface: Color = Color.Black
    var SurfaceHairline: Color = Color(0x1AFFFFFF)
    var hairline: Color = Color(0x1AFFFFFF)
    var TextPrimary: Color = Neutral90

    var TextSecondary: Color = Neutral70
    var TextTertiary: Color = Neutral50
    var TextMuted: Color = Neutral50
    var Text: Color = Neutral90
    var TextOnAccent: Color = Color.White
    var OmniAccentPrimary: Color = Purple70
    var OmniAccentSecondary: Color = Cyan70
    var OmniAccentTertiary: Color = Magenta70
    var OmniAccentWarm: Color = Orange80
    var OmniAccentMuted: Color = Neutral50
    var OmniAccentSoft: Color = Purple30
    var OmniAccentOnPrimary: Color = Color.White
    var ActivePlayback: Color = Purple70
    var Downloaded: Color = Color(0xFF4DD9F0)
    var Hot: Color = Color(0xFFFFB59D)
    var Favorite: Color = Color(0xFFFF7BCB)
    var Queue: Color = Cyan70
    var OmniGlassSubtle: Color = GlassWhite
    var OmniGlassMedium: Color = GlassWhite
    var OmniGlassHeavy: Color = GlassBlack
    var OmniGlassStrong: Color = GlassBlack
    var OmniGlassPlayer: Color = GlassBlack
    var OmniGlassBorderSubtle: Color = Color(0x1AFFFFFF)
    var OmniGlassBorderMedium: Color = Color(0x33FFFFFF)
    var OmniGlassBorderStrong: Color = Color(0x66FFFFFF)
    var OmniGlassGlow: Color = Purple50.copy(alpha = 0.2f)
    var BorderSubtle: Color = Color(0x1AFFFFFF)
    var BorderMedium: Color = Color(0x33FFFFFF)
    var BorderStrong: Color = Color(0x66FFFFFF)
    var Error: Color = Error80
    var Success: Color = Color(0xFF8CF7A9)
    var Warning: Color = Color(0xFFF0C048)
    var CardBackground: Color = SurfaceContainer
    var Divider: Color = NeutralVar30



    fun updateFromTheme(
        primary: Color,
        secondary: Color,
        tertiary: Color,
        pureBlack: Boolean = false
    ) {
        OmniAccentPrimary = primary
        OmniAccentSecondary = secondary
        OmniAccentTertiary = tertiary
        if (pureBlack) {
            OmniBackgroundBase = Color.Black
            Background = Color.Black
        }
    }
}

object OmniShapes {
    val Tiny: RoundedCornerShape = RoundedCornerShape(4.dp)
    val Small: RoundedCornerShape = RoundedCornerShape(12.dp)
    val Medium: RoundedCornerShape = RoundedCornerShape(16.dp)
    val Large: RoundedCornerShape = RoundedCornerShape(24.dp)
    val XL: RoundedCornerShape = RoundedCornerShape(32.dp)
    val ExtraLarge: RoundedCornerShape = RoundedCornerShape(32.dp)
    val Card: RoundedCornerShape = MusicCardShape
    val Circle: RoundedCornerShape = RoundedCornerShape(50)
    val Dock: RoundedCornerShape = SquircleShape
    val Pill: RoundedCornerShape = PillShape
    val Sheet: RoundedCornerShape = SheetShapeToken
    val Button: RoundedCornerShape = PillShape
    val Chip: RoundedCornerShape = ChipShapeToken
    val ArtworkSmall: RoundedCornerShape = RoundedCornerShape(8.dp)
    val ArtworkMedium: RoundedCornerShape = AlbumArtShape
    val ArtworkLarge: RoundedCornerShape = AlbumArtShape
}

object OmniSpacing {
    val hairline: Dp = 1.dp
    val micro: Dp = 2.dp
    val compact: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val section: Dp = 24.dp
    val hero: Dp = 32.dp
    val touchTarget: Dp = 48.dp
    val screen: Dp = 16.dp
    val screenHorizontalCompact: Dp = 16.dp
    val card: Dp = 16.dp
    val cardPadding: Dp = 16.dp

    val None: Dp = 0.dp

    val XSmall: Dp = SpacingTokens.Xs
    @get:JvmName("getCapitalSmall")
    val Small: Dp = SpacingTokens.Sm
    @get:JvmName("getCapitalMedium")
    val Medium: Dp = SpacingTokens.Md
    @get:JvmName("getCapitalLarge")
    val Large: Dp = SpacingTokens.Lg
    val XLarge: Dp = SpacingTokens.Xl
    val XXLarge: Dp = SpacingTokens.Xxl
    val ScreenPadding: Dp = SpacingTokens.Lg
    val ItemSpacing: Dp = SpacingTokens.Md
}

object OmniMotion {
    const val ThumbnailFadeMillis: Int = 200
    val DurationFast: Int = MotionTokens.DurationShort2
    val DurationNormal: Int = MotionTokens.DurationMedium2
    val DurationSlow: Int = MotionTokens.DurationLong2
    val Standard = MotionTokens.Standard
    val Emphasized = MotionTokens.Emphasized


    fun <T> springBouncy(): FiniteAnimationSpec<T> = MotionTokens.springBouncy()
    fun <T> springGentle(): FiniteAnimationSpec<T> = MotionTokens.springGentle()
    fun <T> springSnappy(): FiniteAnimationSpec<T> = MotionTokens.springSnappy()
    fun <T> gentleSpring(): FiniteAnimationSpec<T> = MotionTokens.springGentle()
    fun <T> tweenEmphasized(durationMs: Int = MotionTokens.DurationMedium2): FiniteAnimationSpec<T> =
        MotionTokens.tweenEmphasized(durationMs)

    fun screenEnter(): androidx.compose.animation.EnterTransition = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220))
    fun screenExit(): androidx.compose.animation.ExitTransition = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220))
    fun screenPopEnter(): androidx.compose.animation.EnterTransition = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220))
    fun screenPopExit(): androidx.compose.animation.ExitTransition = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220))

    fun miniPlayerEnter(): androidx.compose.animation.EnterTransition = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.slideInVertically { it / 2 }
    fun miniPlayerExit(): androidx.compose.animation.ExitTransition = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.slideOutVertically { it / 2 }
}

const val FastFadeMillis: Int = 180
const val PressMillis: Int = 120

data class ThemeSeedPalette(
    val primary: Color = Purple70,
    val secondary: Color = Cyan70,
    val tertiary: Color = Magenta70,
    val neutral: Color = Color.DarkGray
)

object ThemeSeedPaletteCodec {
    fun encodeForPreference(palette: ThemeSeedPalette, name: String = "Default"): String = name
}

data class OmniDynamicSongPalette(
    val background: Color = Color.Black,
    val backgroundSecondary: Color = Color(0xFF151515),
    val surface: Color = Color(0xFF1E1E1E),
    val surfaceElevated: Color = Color(0xFF282828),
    val accent: Color = Purple70,
    val accentSoft: Color = Purple30,
    val miniPlayerSurface: Color = Color(0xFF1E1E1E),
    val playerControlSurface: Color = Color(0xFF282828),
    val gradientStart: Color = Purple70.copy(alpha = 0.4f),
    val gradientEnd: Color = Color.Black
) {
    companion object {
        fun fallback(accent: Color = Purple70) = OmniDynamicSongPalette(accent = accent)
        fun fromArtworkColors(colors: List<Color>, fallbackAccent: Color = Purple70) = OmniDynamicSongPalette(
            accent = colors.firstOrNull() ?: fallbackAccent,
            gradientStart = colors.firstOrNull()?.copy(alpha = 0.4f) ?: fallbackAccent.copy(alpha = 0.4f),
            gradientEnd = colors.lastOrNull() ?: Color.Black
        )
    }
}


@Immutable
data class OmniScheme(
    val accent: Color = Purple70,
    val accentSecondary: Color = Cyan70,
    val accentTertiary: Color = Magenta70,
    val textPrimary: Color = Neutral90,
    val textSecondary: Color = Neutral70,

    val textTertiary: Color = Neutral50,
    val textOnAccent: Color = Color.White,
    val background: Color = Color.Black,
    val backgroundElevated: Color = SurfaceContainerHigh,
    val surface: Color = Color.Black,
    val surfaceContainer: Color = SurfaceContainer,
    val surfaceRaised: Color = SurfaceContainerHigh,
    val surfaceFloating: Color = SurfaceContainerHighest,
    val surfaceSubtle: Color = SurfaceContainerLowest,
    val surfaceQuiet: Color = SurfaceContainerLowest,
    val surfacePanel: Color = SurfaceContainer,
    val surfaceCard: Color = SurfaceContainer,
    val outline: Color = NeutralVar60,
    val divider: Color = NeutralVar30,
    val cardBackground: Color = SurfaceContainer,
    val activePlayback: Color = Purple70,
    val error: Color = Error80,
    val warning: Color = Color(0xFFF0C048),
    val glassSubtle: Color = GlassWhite,
    val glassMedium: Color = GlassWhite,
    val glassHeavy: Color = GlassBlack,
    val glassBorderSubtle: Color = Color(0x1AFFFFFF),
    val glassBorderMedium: Color = Color(0x33FFFFFF),
    val glassBorderStrong: Color = Color(0x66FFFFFF),
    val glassGlow: Color = Purple50.copy(alpha = 0.2f),
    val borderSubtle: Color = Color(0x1AFFFFFF),
    val borderStrong: Color = Color(0x66FFFFFF),
    val hairline: Color = Color(0x1AFFFFFF),
    val accentGradient: Brush = Brush.horizontalGradient(listOf(Purple50, Cyan50)),


) {
    companion object {
        fun dark(accent: Color = Purple70, pureBlack: Boolean = false) = OmniScheme(
            accent = accent,
            background = if (pureBlack) Color.Black else Neutral10,
            surface = if (pureBlack) Color.Black else Neutral10
        )
        fun light(accent: Color = Purple40) = OmniScheme(
            accent = accent,
            textPrimary = Neutral10,
            textSecondary = Neutral40,
            textTertiary = Neutral60,
            background = Neutral99,
            surface = Neutral99
        )
    }
}

val LocalOmniColors = staticCompositionLocalOf { OmniScheme() }

@Composable
fun omniColors(): OmniScheme = LocalOmniColors.current

data class OmniDynamicAccents(
    val primary: Color = Purple70,
    val secondary: Color = Cyan70,
    val tertiary: Color = Magenta70,
    val glow: Color = Purple50.copy(alpha = 0.30f),
    val soft: Color = Purple30,
)

val LocalOmniAccents = staticCompositionLocalOf { OmniDynamicAccents() }

object PlayerColorExtractor {
    object Config {
        const val MAX_COLOR_COUNT: Int = 48
        const val BITMAP_AREA: Int = 10000
        const val DEFAULT_MAX_COLORS: Int = 48
        const val DEFAULT_MUTED_FALLBACK: Int = 0xFF1D0035.toInt()
        const val IMAGE_SIZE: Int = 120
    }
    fun extractGradientColors(palette: Any? = null, fallbackColor: Int = 0): List<Color> = listOf(Color(0xFF1D0035), Color(0xFF0F0F0F))
    fun extractGradientColors(bitmap: Bitmap? = null, palette: Any? = null, fallbackColor: Color = Color(0xFF1D0035)): List<Color> = listOf(Color(0xFF1D0035), Color(0xFF0F0F0F))
    fun extractThemeColor(palette: Any? = null, fallbackColor: Int = 0): Color = Purple70
    fun extractThemeColor(bitmap: Bitmap? = null, palette: Any? = null, fallbackColor: Color = Color(0xFF1D0035)): Color = Purple70
    fun extractPlayerPalette(bitmap: Bitmap? = null, palette: Any? = null, fallbackColor: Color = Color(0xFF1D0035)): List<Color> = listOf(Purple70, Cyan70, Magenta70)
}

fun Modifier.omniGlassSurface(): Modifier = this
fun Modifier.omniSoftBorder(): Modifier = this

val OmniScheme.accentSoft: Color get() = accentSecondary
val OmniScheme.textDisabled: Color get() = textTertiary

val AppFontFamily: FontFamily = Outfit

fun omniTypography(fontFamily: FontFamily = Outfit): Typography = Typography

object OmniTextStyles {
    val eyebrow = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    val songTitle = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    val sectionHeader = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    )
    val displayTitle = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    )
    val caption = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    val metadata = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    val body = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    val title = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    val headline = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    )
    val largeTitle = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    )
}
