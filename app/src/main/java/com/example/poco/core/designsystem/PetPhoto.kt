package com.example.poco.core.designsystem

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.example.poco.core.theme.MungoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 반려견 얼굴. 우선순위: 등록한 사진 → 품종 아바타(품종 입력값) → mungo 발바닥.
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

            // 사진도 품종 아바타도 없으면 mungo 발바닥
            else ->
                Box(
                    modifier = circle.background(MungoTheme.colors.bgAccentSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    MungoIconView(
                        MungoIcon.PAW_FILLED,
                        tint = MungoTheme.colors.textAccent,
                        modifier = Modifier.fillMaxSize(0.45f)
                    )
                }
        }
    }
}

/** 동그라미를 꽉 채우는 이미지(사진 · 품종 아바타)인지. 아니면 기본 마크라 조금 작게 그린다 */
fun hasFullPetImage(photoPath: String?, breed: String?): Boolean =
    photoPath != null || BreedAvatar.from(breed) != null

/**
 * mungo 시츄 일러스트 (Figma 페이지 4 홈 510:137 · 탑승 준비 4 488:9017).
 * 유저가 등록한 사진 · 품종과 상관없이 늘 같은 그림을 보여 준다.
 * 배경 없는 원본이라 자르지 않고 칸 안에 맞춰 그린다.
 *
 * @param standing true면 서 있는 시츄 (Figma 홈2 · 차량 연동 해지 488:11766)
 */
@Composable
fun ShihTzuIllustration(modifier: Modifier = Modifier, standing: Boolean = false) {
    Image(
        painter = painterResource(
            if (standing) {
                com.example.poco.R.drawable.mungo_shih_tzu_standing
            } else {
                com.example.poco.R.drawable.mungo_shih_tzu
            }
        ),
        contentDescription = "시츄",
        contentScale = ContentScale.Fit,
        alignment = Alignment.BottomCenter,
        modifier = modifier
    )
}
