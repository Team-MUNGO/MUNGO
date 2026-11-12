package com.example.poco.core.designsystem

import androidx.annotation.DrawableRes
import com.example.poco.R

/**
 * 품종 아바타 (원본: design/brand 폴더의 품종 PNG → 원형으로 잘라 res/drawable-nodpi 의 breed_ 이미지)
 * 사진이 없을 때 "종류" 입력값으로 골라 보여준다. 모르는 품종이면 null → 기본 강아지 마크
 */
enum class BreedAvatar(
    @DrawableRes val drawable: Int,
    val displayName: String,
    private val keywords: List<String>
) {
    MALTESE(R.drawable.breed_maltese, "몰티즈", listOf("몰티즈", "말티즈", "maltese")),
    TOY_POODLE(R.drawable.breed_toy_poodle, "토이푸들", listOf("푸들", "poodle")),
    POMERANIAN(R.drawable.breed_pomeranian, "포메라니안", listOf("포메", "pomeranian")),
    BICHON(R.drawable.breed_bichon, "비숑 프리제", listOf("비숑", "bichon")),
    GOLDEN_RETRIEVER(
        R.drawable.breed_golden_retriever,
        "골든 리트리버",
        listOf("골든", "리트리버", "golden", "retriever")
    ),
    WELSH_CORGI(R.drawable.breed_welsh_corgi, "웰시코기", listOf("코기", "corgi")),
    SHIBA_INU(R.drawable.breed_shiba_inu, "시바견", listOf("시바", "shiba"));

    companion object {
        /** "골든 리트리버", "골든리트리버", "Golden" 처럼 띄어쓰기·대소문자가 달라도 찾는다 */
        fun from(breed: String?): BreedAvatar? {
            val normalized =
                breed?.replace(" ", "")?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull { avatar -> avatar.keywords.any { normalized.contains(it) } }
        }
    }
}
