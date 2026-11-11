package com.example.poco.presentation.petprofile

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.poco.R
import com.example.poco.core.designsystem.PocoIcon
import com.example.poco.core.designsystem.PocoIconView
import com.example.poco.core.designsystem.component.PocoButton
import com.example.poco.core.designsystem.component.PocoButtonStyle
import com.example.poco.core.theme.POCOTheme
import com.example.poco.core.theme.PocoRadius
import com.example.poco.core.theme.PocoSize
import com.example.poco.core.theme.PocoSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 차량 화면용 큰 사진 고르기.
 * 시스템 사진 선택기는 차량 화면(저밀도)에서 글자·사진이 너무 작게 나와서,
 * 앱 안에서 큰 타일(최소 240dp)로 보여준다. 권한이 없거나 사진이 없으면 시스템 선택기로 넘어갈 수 있다.
 */
@Composable
fun PetPhotoPickerDialog(
    onPicked: (String) -> Unit,
    onOpenSystemPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasImagePermission()) }
    var askedPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        askedPermission = true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(imagePermission())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val colors = POCOTheme.colors
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .fillMaxSize(0.88f)
                .background(colors.backgroundCard, RoundedCornerShape(PocoRadius.r36))
                .padding(PocoSpacing.space48)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "펫 사진 고르기",
                    style = POCOTheme.typography.headingL,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                PocoButton(
                    text = "기본 선택기",
                    onClick = onOpenSystemPicker,
                    style = PocoButtonStyle.SECONDARY
                )
                Spacer(Modifier.size(PocoSpacing.space16))
                PocoButton(text = "닫기", onClick = onDismiss, style = PocoButtonStyle.SECONDARY)
            }
            Spacer(Modifier.height(PocoSpacing.space32))
            PhotoGrid(
                hasPermission = hasPermission,
                askedPermission = askedPermission,
                onPicked = onPicked,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 맨 앞은 앱에 들어 있는 예시 사진, 그 뒤로 차에 저장된 사진 (권한이 있을 때) */
@Composable
private fun PhotoGrid(
    hasPermission: Boolean,
    askedPermission: Boolean,
    onPicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sample = remember { context.samplePhotoUri() }
    val photos by produceState<List<Uri>?>(initialValue = null, hasPermission) {
        value = if (hasPermission) {
            withContext(Dispatchers.IO) { context.queryRecentImages() }
        } else {
            null
        }
    }
    val note = when {
        hasPermission && photos?.isEmpty() == true ->
            "차에 저장된 사진이 아직 없어요 · 예시 사진으로 먼저 해 볼 수 있어요"
        !hasPermission && askedPermission ->
            "사진 권한이 없어서 예시 사진만 보여요 · ‘기본 선택기’로 다른 사진을 고를 수 있어요"
        else -> null
    }
    Column(modifier = modifier) {
        note?.let {
            Text(
                text = it,
                style = POCOTheme.typography.bodyL,
                color = POCOTheme.colors.textSecondary
            )
            Spacer(Modifier.height(PocoSpacing.space24))
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = PHOTO_TILE_MIN),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PocoSpacing.space24),
            verticalArrangement = Arrangement.spacedBy(PocoSpacing.space24)
        ) {
            item(key = "sample") {
                PhotoTile(uri = sample, label = "예시 사진", onClick = { onPicked(sample.toString()) })
            }
            items(photos.orEmpty(), key = { it.toString() }) { uri ->
                PhotoTile(uri = uri, onClick = { onPicked(uri.toString()) })
            }
        }
    }
}

@Composable
private fun PhotoTile(uri: Uri, onClick: () -> Unit, label: String? = null) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { context.loadThumbnail(uri) }
    }
    val shape = RoundedCornerShape(PocoRadius.r24)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(POCOTheme.colors.fieldBackground, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = label ?: "펫 사진 후보",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        label?.let {
            Text(
                text = it,
                style = POCOTheme.typography.caption,
                color = POCOTheme.colors.onAccentFill,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(PocoSpacing.space12)
                    .background(POCOTheme.colors.accentFill, CircleShape)
                    .padding(horizontal = PocoSpacing.space16, vertical = PocoSpacing.space4)
            )
        }
    }
}

/** 앱에 들어 있는 예시 펫 사진 (res/raw) - 사진이 없는 차에서도 바로 해 볼 수 있게 */
private fun Context.samplePhotoUri(): Uri =
    Uri.parse("android.resource://$packageName/${R.raw.sample_pet_profile}")

@Composable
private fun EmptyMessage(title: String, description: String) {
    val colors = POCOTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PocoSpacing.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(colors.fieldBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            PocoIconView(
                PocoIcon.CAMERA,
                tint = colors.textSecondary,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(PocoSpacing.space32))
        Text(text = title, style = POCOTheme.typography.headingM, color = colors.textPrimary)
        Spacer(Modifier.height(PocoSpacing.space12))
        Text(
            text = description,
            style = POCOTheme.typography.bodyL,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = POCOTheme.colors.accentFill,
            modifier = Modifier.size(PocoSize.touchMin)
        )
    }
}

private val PHOTO_TILE_MIN = 240.dp
private const val MAX_PHOTOS = 300
private const val THUMBNAIL_PX = 480

private fun imagePermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.READ_MEDIA_IMAGES
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}

private fun Context.hasImagePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, imagePermission()) == PackageManager.PERMISSION_GRANTED

/** 최근 사진부터 */
private fun Context.queryRecentImages(): List<Uri> = runCatching {
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    contentResolver.query(
        collection,
        arrayOf(MediaStore.Images.Media._ID),
        null,
        null,
        "${MediaStore.Images.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        buildList {
            while (cursor.moveToNext() && size < MAX_PHOTOS) {
                add(ContentUris.withAppendedId(collection, cursor.getLong(idColumn)))
            }
        }
    }.orEmpty()
}.getOrDefault(emptyList())

private fun Context.loadThumbnail(uri: Uri): ImageBitmap? = runCatching {
    // 미리보기 썸네일은 MediaStore(content://) 사진만 가능 → 예시 사진은 줄여서 직접 읽는다
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
        contentResolver.loadThumbnail(uri, Size(THUMBNAIL_PX, THUMBNAIL_PX), null)
    } else {
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }?.asImageBitmap()
}.getOrNull()
