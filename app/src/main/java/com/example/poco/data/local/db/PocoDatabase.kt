package com.example.poco.data.local.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PetEntity::class, TripEntity::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        // 반려견 창문 알림 기준(%) 추가
        AutoMigration(from = 2, to = 3),
        // 반려견 사진 경로 추가
        AutoMigration(from = 3, to = 4),
        // 고정 방식(이동장 · 하네스) 추가 - POCO 기준점 ③
        AutoMigration(from = 4, to = 5)
    ]
)
abstract class PocoDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao

    abstract fun tripDao(): TripDao
}
