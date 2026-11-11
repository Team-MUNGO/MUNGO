package com.example.poco.core.designsystem

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 반려견 얼굴. 우선순위: 등록한 사진 → 품종 아바타(종류 입력값) → 기본 강아지 마크.
 * 사진은 앱이 512px로 줄여 저장해 두므로 바로 읽어도 가볍다.
 */
@Composable
fun PetPhoto(photoPath: String?, modifier: Modifier = Modifier, breed: String? = null) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photoPath) {
        value = photoPath?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }
    val breedAvatar = BreedAvatar.from(breed)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val image = bitmap
        val circle = Modifier
            .fillMaxSize()
            .clip(CircleShape)
        when {
            image != null ->
                Image(
                    bitmap = image,
                    contentDescription = "반려견 사진",
                    contentScale = ContentScale.Crop,
                    modifier = circle
                )

            breedAvatar != null ->
                Image(
                    painter = painterResource(breedAvatar.drawable),
                    contentDescription = breedAvatar.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = circle
                )

            else -> PocoMark(modifier = Modifier.fillMaxSize())
        }
    }
}

/** 동그라미를 꽉 채우는 이미지(사진 · 품종 아바타)인지. 아니면 기본 마크라 조금 작게 그린다 */
fun hasFullPetImage(photoPath: String?, breed: String?): Boolean =
    photoPath != null || BreedAvatar.from(breed) != null
