package com.omnitune.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.models.LogoVariant

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    variant: LogoVariant = LogoVariant.DEFAULT,
    contentDescription: String? = "OmniTune",
    contentScale: ContentScale = ContentScale.Fit,
    colorFilter: ColorFilter? = null,
) {
    Image(
        painter = painterResource(id = variant.drawableRes()),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
    )
}

@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    AppLogo(modifier = modifier.size(size))
}
