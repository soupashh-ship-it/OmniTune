package com.omnitune.app.ui.screens.search

import com.omnitune.app.ui.screens.SearchViewModel
import com.omnitune.app.ui.screens.SearchStatus
import com.omnitune.app.ui.screens.SearchUiState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.omnitune.app.R
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.OmniFloatingSurface
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniPressScale
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import timber.log.Timber

@Composable
fun SearchTopBar(
    query: TextFieldValue,
    isSearching: Boolean,
    onQueryChange: (TextFieldValue) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = OmniSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchIconButton(
                icon = R.drawable.ic_arrow_back,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(modifier = Modifier.width(OmniSpacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Search",
                    style = OmniTextStyles.screenTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Find songs, artists, albums, and playlists",
                    style = OmniTextStyles.metadata,
                    color = OmniColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        OmniFloatingSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(OmniChrome.SearchBarHeight)
                .border(
                    width = 1.dp,
                    color = OmniColors.SurfaceHairline,
                    shape = OmniShapes.ExtraLarge,
                ),
            shape = OmniShapes.ExtraLarge,
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxSize(),
                placeholder = {
                    Text(
                        text = "Search songs, artists, albums...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmniColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = OmniColors.OmniAccentSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                },
                trailingIcon = {
                    when {
                        isSearching -> OmniTuneLoader(size = 22.dp, color = OmniColors.ActivePlayback)
                        query.text.isNotEmpty() -> IconButton(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                onClear()
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Clear search",
                                tint = OmniColors.TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = OmniColors.OmniAccentPrimary,
                    focusedTextColor = OmniColors.TextPrimary,
                    unfocusedTextColor = OmniColors.TextPrimary,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = OmniColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus(force = true) },
                ),
                singleLine = true,
            )
        }
    }
}


@Composable
fun SearchIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceQuiet)
            .omniPressScale(interactionSource),
        interactionSource = interactionSource,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = OmniColors.TextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}
