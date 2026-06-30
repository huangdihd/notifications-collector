package com.noticol.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(entity: NotificationEntity): Long

    /** UI 用：按时间倒序观察全部记录 */
    @Query("SELECT * FROM notifications ORDER BY postTime DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    /** 导出用：一次性取全部（可选按包名过滤） */
    @Query("SELECT * FROM notifications ORDER BY postTime ASC")
    suspend fun getAll(): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE packageName IN (:packages) ORDER BY postTime ASC")
    suspend fun getByPackages(packages: List<String>): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE postTime < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long): Int
}
