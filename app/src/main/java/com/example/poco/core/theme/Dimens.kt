package com.example.poco.core.theme

import androidx.compose.ui.unit.dp

/*
 * 디자인 시스템 DS-03 Spacing & Layout (1920×1080 · 밀도 160 → 시안 px = dp)
 */

/** 정수 px · 4 단위 기본, 2는 미세 조정용 */
object PocoSpacing {
    val space2 = 2.dp // 아이콘-글자 미세 간격
    val space4 = 4.dp // 라벨-값
    val space8 = 8.dp // 라벨-입력칸 · 칩 사이
    val space12 = 12.dp // 세그먼트 사이 · 상태 줄 사이
    val space16 = 16.dp // 카드 사이 · 입력칸 사이
    val space20 = 20.dp // 카드 안 요소 사이
    val space24 = 24.dp // 카드 안쪽 여백 · 열 간격
    val space28 = 28.dp // 앱 창 안쪽 여백 · 홈 열 간격
    val space32 = 32.dp // 큰 카드 안쪽 · 프로필 열 간격
    val space40 = 40.dp // 프로필 카드 안쪽
    val space48 = 48.dp // 화면 위·아래 여백
    val space96 = 96.dp // 전체 화면 좌우 여백
}

/** 0.5px 단위 허용 */
object PocoStroke {
    val card = 1.5.dp // 카드 외곽 · 표 구분선
    val control = 2.dp // 입력칸 · 칩 · 보조 버튼
    val dashed = 2.dp // 추가 버튼 · 빈 사진 (대시 8 / 간격 6)
    const val DASH_ON = 8f
    const val DASH_OFF = 6f
    val focus = 3.dp // 입력칸 포커스
    val focusRing = 6.dp // 포커스 링 (lime-soft)
}

/** 정수 px */
object PocoRadius {
    val r8 = 8.dp // 게이지
    val r12 = 12.dp // PRND · 작은 버튼
    val r16 = 16.dp // 입력칸 · 아이콘 타일
    val r18 = 18.dp // 세그먼트 · 아이콘 타일(64)
    val r20 = 20.dp // 통계 타일 · 목록
    val r22 = 22.dp // 일반 버튼
    val r24 = 24.dp // 카드 · 앱 창 · 큰 버튼 · 상태 줄
    val r28 = 28.dp // 메인 카드
    val r36 = 36.dp // 가운데 배너
}

/** 터치 · 아이콘 · 컴포넌트 크기 */
object PocoSize {
    val touchMin = 76.dp // 모든 버튼 · 칩 · 키 (Android for Cars 기준)
    val buttonHeight = 76.dp // 일반 버튼
    val buttonHeightLarge = 96.dp // 화면 상단 주요 버튼
    val icon24 = 24.dp // 선 2.2px · 24 그리드
    val icon28 = 28.dp
    val icon32 = 32.dp
    val iconTile = 56.dp // 라운드 16
    val iconTileLarge = 64.dp // 라운드 18
    val toggleWidth = 96.dp
    val toggleHeight = 56.dp
    val toggleKnob = 44.dp
    val segmentHeight = 80.dp
    val inputHeight = 76.dp
    val chipHeight = 40.dp // 32~40
    val statusCircle = 44.dp // 상태 원 40~44
    val avatarList = 60.dp // 목록
    val avatarHome = 128.dp // 홈
    val avatarProfile = 240.dp // 프로필
    val infoRowHeight = 92.dp // 상태 줄
    val voiceCardHeight = 116.dp
    val alertCardHeight = 232.dp
    val scoreRingSmall = 220.dp // 선 22
    val scoreRingLarge = 320.dp // 선 26
}

/** 화면 레이아웃 */
object PocoLayout {
    // 정차 화면 (홈 · 펫 프로필, 전체 화면)
    val statusBarHeight = 88.dp
    val screenHorizontal = 96.dp // 좌우
    val screenTop = 32.dp // 상태 바(88) 아래 32 → 위 120
    val screenBottom = 48.dp
    val columnGap = 28.dp // 홈 열 간격
    const val HOME_COLUMN_LEFT = 620f
    const val HOME_COLUMN_MIDDLE = 636f
    const val HOME_COLUMN_RIGHT = 416f
    const val PROFILE_COLUMN_LEFT = 520f
    const val PROFILE_COLUMN_RIGHT = 1176f
    val profileColumnGap = 32.dp

    // 주행 중 (Pleos 프레임 · 앱 창 1271×884)
    val drivingWindowPadding = 28.dp
    val drivingHeaderHeight = 96.dp
    val drivingColumnGap = 24.dp
    val drivingCardGap = 16.dp // 상태 줄은 12
}
