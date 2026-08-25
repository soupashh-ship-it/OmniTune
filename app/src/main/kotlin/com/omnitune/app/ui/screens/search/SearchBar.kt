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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniColors
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
    focusRequester: FocusRequester,
    onQueryChange: (TextFieldValue) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
        if (query.text.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = OmniSpacing.micro),
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
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Find your music, your way.",
                        style = MaterialTheme.typography.bodySmall,
                        color = omniColors().textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (query.text.isNotBlank()) {
                SearchIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = "Back",
                    onClick = onBack,
                )
                Spacer(modifier = Modifier.width(OmniSpacing.compact))
            }
            SearchQueryField(
                query = query,
                isSearching = isSearching,
                focusRequester = focusRequester,
                onQueryChange = onQueryChange,
                onClear = onClear,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SearchQueryField(
    query: TextFieldValue,
    isSearching: Boolean,
    focusRequester: FocusRequester,
    onQueryChange: (TextFieldValue) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    OmniFloatingSurface(
        modifier = modifier
            .height(44.dp)
            .border(
                width = 1.dp,
                color = omniColors().accent.copy(alpha = 0.36f),
                shape = OmniShapes.ExtraLarge,
            ),
        shape = OmniShapes.ExtraLarge,
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = "Search songs, albums, artists, playlists...",
                    style = MaterialTheme.typography.bodySmall,
                    color = omniColors().textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                when {
                    isSearching -> OmniTuneLoader(size = 22.dp, color = omniColors().accent)
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
                            tint = omniColors().textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    else -> Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = omniColors().textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = omniColors().accent,
                focusedTextColor = omniColors().textPrimary,
                unfocusedTextColor = omniColors().textPrimary,
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = omniColors().textPrimary,
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
            .size(36.dp)
            .clip(OmniShapes.Pill)
            .background(omniColors().surfaceQuiet.copy(alpha = 0.42f))
            .border(1.dp, omniColors().accent.copy(alpha = 0.32f), OmniShapes.Pill)
            .omniPressScale(interactionSource),
        interactionSource = interactionSource,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = omniColors().textPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}
