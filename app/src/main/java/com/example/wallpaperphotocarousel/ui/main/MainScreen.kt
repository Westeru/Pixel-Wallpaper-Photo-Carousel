package com.example.wallpaperphotocarousel.ui.main

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.wallpaperphotocarousel.CarouselWallpaperService
import com.example.wallpaperphotocarousel.data.DefaultDataRepository
import com.example.wallpaperphotocarousel.theme.WallpaperPhotoCarouselTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current.applicationContext
  val viewModel: MainScreenViewModel = viewModel {
    MainScreenViewModel(DefaultDataRepository(context))
  }
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  when (state) {
    MainScreenUiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is MainScreenUiState.Success -> {
      MainDashboard(
        data = (state as MainScreenUiState.Success).data,
        viewModel = viewModel,
        modifier = modifier
      )
    }
    is MainScreenUiState.Error -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = "Error loading data: ${(state as MainScreenUiState.Error).throwable.message}",
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(16.dp)
        )
      }
    }
  }
}

@Composable
internal fun MainDashboard(
  data: List<String>,
  viewModel: MainScreenViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var isWallpaperActive by remember { mutableStateOf(false) }

  // Check wallpaper status whenever visibility changes or on startup
  LaunchedEffect(Unit) {
    isWallpaperActive = checkWallpaperStatus(context)
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100),
    onResult = { uris ->
      if (uris.isNotEmpty()) {
        viewModel.addUris(context, uris)
      }
    }
  )

  Column(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header Section
    Column {
      Text(
        text = "Photo Shuffle",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp
        )
      )
      Text(
        text = "Pixel Edition",
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
        )
      )
    }

    // Status Info Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(
        containerColor = if (isWallpaperActive) {
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }
      ),
      shape = RoundedCornerShape(16.dp)
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (isWallpaperActive) Icons.Default.CheckCircle else Icons.Default.Info,
          contentDescription = null,
          tint = if (isWallpaperActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = if (isWallpaperActive) "Carousel Wallpaper Active" else "Setup Required",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = if (isWallpaperActive) {
              "Your wallpapers will shuffle smoothly on device unlock."
            } else {
              "Set this live wallpaper to enable shuffle on unlock."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Selection Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Selected Collection",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${data.size} photo(s)",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.secondary
        )
        if (data.isNotEmpty()) {
          Spacer(modifier = Modifier.width(8.dp))
          TextButton(
            onClick = { viewModel.clearAll(context) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete all photos",
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete All", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }

    // Photo Carousel Grid or Empty State
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      if (data.isEmpty()) {
        Card(
          modifier = Modifier.fillMaxSize(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
          shape = RoundedCornerShape(20.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Image,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "No Photos Selected",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Tap 'Select Photos' below to select background wallpapers from your gallery.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
          }
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Fixed(3),
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(data, key = { it }) { uriStr ->
            PhotoGridItem(
              uriString = uriStr,
              onDeleteClick = { viewModel.removeUri(context, uriStr) }
            )
          }
        }
      }
    }

    // Bottom Action Row
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Select Photos", fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = {
          launchWallpaperChooser(context)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
      ) {
        Icon(imageVector = Icons.Default.Wallpaper, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Activate Live Wallpaper")
      }
    }
  }
}

@Composable
fun PhotoGridItem(
  uriString: String,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(12.dp))
  ) {
    UriThumbnail(
      uriString = uriString,
      modifier = Modifier.fillMaxSize()
    )

    // Delete Button Overlay
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(6.dp),
      contentAlignment = Alignment.TopEnd
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .background(Color.Black.copy(alpha = 0.6f), CircleShape)
          .clip(CircleShape)
      ) {
        IconButton(
          onClick = onDeleteClick,
          modifier = Modifier.fillMaxSize()
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete photo",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
fun UriThumbnail(
  uriString: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(uriString) {
    bitmap = withContext(Dispatchers.IO) {
      try {
        val uri = Uri.parse(uriString)
        val options = BitmapFactory.Options().apply {
          inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, options)
        }

        val targetSize = 300 // dp-ish bounds
        var inSampleSize = 1
        if (options.outWidth > targetSize || options.outHeight > targetSize) {
          val halfWidth = options.outWidth / 2
          val halfHeight = options.outHeight / 2
          while ((halfWidth / inSampleSize) >= targetSize && (halfHeight / inSampleSize) >= targetSize) {
            inSampleSize *= 2
          }
        }

        val decodeOptions = BitmapFactory.Options().apply {
          this.inSampleSize = inSampleSize
          inPreferredConfig = Bitmap.Config.RGB_565 // More memory efficient
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
      } catch (e: Exception) {
        null
      }
    }
  }

  if (bitmap != null) {
    Image(
      bitmap = bitmap!!.asImageBitmap(),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = modifier
    )
  } else {
    Box(
      modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Image,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
      )
    }
  }
}

private fun checkWallpaperStatus(context: Context): Boolean {
  val wm = WallpaperManager.getInstance(context)
  val info = wm.wallpaperInfo
  return info != null && info.packageName == context.packageName
}

private fun launchWallpaperChooser(context: Context) {
  val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
    putExtra(
      WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
      ComponentName(context, CarouselWallpaperService::class.java)
    )
  }
  try {
    context.startActivity(intent)
  } catch (e: Exception) {
    val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
    try {
      context.startActivity(fallbackIntent)
    } catch (ex: Exception) {
      Toast.makeText(context, "Could not open Live Wallpaper settings", Toast.LENGTH_LONG).show()
    }
  }
}

