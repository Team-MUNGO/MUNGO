package com.example.poco.data.local.datasource

interface PetPhotoDataSource {
    /**
     * 사진 선택기에서 받은 이미지를 앱 내부 저장소로 복사한다.
     * 선택기 URI는 권한이 잠깐만 유지되므로 반드시 복사해 둔다.
     * @return 저장된 파일 경로
     */
    suspend fun importPhoto(uri: String): String

    suspend fun deletePhoto(path: String)
}
