package com.example.poco.presentation.permission

import com.example.poco.core.designsystem.PocoIcon
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet

data class PermissionUiState(
    /** 아직 반려견 등록 전이라 기본 이름 */
    val petName: String = DEFAULT_PET_NAME,
    /** 시안: 필수 3개는 켜짐, 선택(음성)은 꺼짐으로 시작 */
    val enabledGroups: ImmutableSet<PermissionGroup> =
        PermissionGroup.entries.filter { it.isRequired }.toPersistentSet(),
    /** 필수 항목을 끄려고 할 때 보여줄 배너 */
    val requiredBanner: PermissionGroup? = null,
    val isCompleted: Boolean = false
) {
    companion object {
        const val DEFAULT_PET_NAME = "현댕이"
    }
}

/** 권한 동의 화면의 한 줄 = 데이터 묶음 하나. "무엇을 해주는지"를 한 줄로 설명한다. */
enum class PermissionGroup(
    val icon: PocoIcon,
    val title: String,
    /** {name} = 반려견 이름 */
    val description: String,
    val permissions: List<String>,
    /** 필수 3개(속도·조향 / 창문 / 공조 온도) · 선택 1개(음성) */
    val isRequired: Boolean = true,
    /** 필수 배너 제목. 예) 창문 정보는 꼭 필요해요 */
    val requiredTitle: String = ""
) {
    DRIVING(
        icon = PocoIcon.CAR,
        title = "속도 · 조향 · 브레이크",
        description = "흔들림을 컴포트 점수로 바꿔요",
        requiredTitle = "주행 정보는 꼭 필요해요",
        permissions = listOf(
            "pleos.car.permission.CAR_INFO",
            "pleos.car.permission.CAR_DRIVE",
            "pleos.car.permission.FUSED_LOCATION",
            "pleos.car.permission.NAVI_ROUTE",
            "pleos.car.permission.NAVI_CUSTOM_MAP",
            "android.car.permission.CAR_SPEED",
            "android.car.permission.CAR_POWERTRAIN"
        )
    ),
    WINDOW(
        icon = PocoIcon.WINDOW,
        title = "창문 상태",
        description = "{name} 옆 창문이 열리면 바로 알려요",
        requiredTitle = "창문 정보는 꼭 필요해요",
        permissions = listOf(
            "pleos.car.permission.CAR_WINDOWS",
            "pleos.car.permission.CONTROL_CAR_WINDOWS"
        )
    ),
    CLIMATE(
        icon = PocoIcon.THERMOMETER,
        title = "공조 온도",
        description = "뒷좌석이 덥거나 추우면 알려요",
        requiredTitle = "공조 온도는 꼭 필요해요",
        permissions = listOf("pleos.car.permission.CAR_CLIMATE")
    ),
    VOICE(
        icon = PocoIcon.SPEAKER,
        title = "음성 안내",
        description = "화면 대신 차 스피커로 말해요",
        isRequired = false,
        permissions = listOf(
            "pleos.car.permission.TTS_SERVICE",
            "pleos.car.permission.STT_SERVICE"
        )
    );

    fun descriptionFor(petName: String) = description.replace("{name}", petName)
}
