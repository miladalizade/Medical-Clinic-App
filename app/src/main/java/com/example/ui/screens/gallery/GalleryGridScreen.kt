package com.example.ui.screens.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.GalleryImage
import com.example.data.repository.DefaultData
import com.example.ui.components.ClinicTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.PastelSkyBlue
import com.example.ui.theme.PastelSkyBlueText
import com.example.ui.viewmodel.ClinicViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun GalleryGridScreen(
    albumId: String,
    viewModel: ClinicViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val galleryState by viewModel.galleryState.collectAsState()
    val selectedAlbumState by viewModel.selectedAlbum.collectAsState()

    val album = selectedAlbumState?.takeIf { it.id == albumId }
        ?: when (val state = galleryState) {
            is UiState.Success -> state.data.find { it.id == albumId }
            else -> DefaultData.galleryAlbums.find { it.id == albumId }
        } ?: DefaultData.galleryAlbums.first()

    var showFullscreenViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            ClinicTopBar(
                title = album.title,
                subtitle = "${album.images.size} تصویر اختصاصی",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .testTag("gallery_grid_screen")
        ) {
            if (album.images.isEmpty()) {
                EmptyStateView(
                    title = "تصویری وجود ندارد",
                    description = "هنوز تصویری به این دسته اضافه نشده است."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (album.description.isNotBlank()) {
                        item(span = { GridItemSpan(2) }) {
                            Surface(
                                color = PastelSkyBlue.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = album.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    itemsIndexed(album.images, key = { _, img -> img.id }) { index, image ->
                        GalleryGridItem(
                            image = image,
                            onClick = {
                                selectedImageIndex = index
                                showFullscreenViewer = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFullscreenViewer && album.images.isNotEmpty()) {
        FullscreenImageViewer(
            images = album.images,
            initialIndex = selectedImageIndex,
            onDismiss = { showFullscreenViewer = false }
        )
    }
}

@Composable
fun GalleryGridItem(
    image: GalleryImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("gallery_grid_item_${image.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.resolvedUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = image.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = image.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (image.caption.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = image.caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

