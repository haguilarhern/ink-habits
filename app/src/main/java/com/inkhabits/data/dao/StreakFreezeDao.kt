package com.inkhabits.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkhabits.data.entity.StreakFreeze
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakFreezeDao {
    @Query("SELECT * FROM streak_freezes")
    fun observeAll(): Flow<List<StreakFreeze>>

    @Query("SELECT * FROM streak_freezes")
    suspend fun getAll(): List<StreakFreeze>

    @Query("SELECT * FROM streak_freezes WHERE habitId = :habitId")
    suspend fun getForHabit(habitId: Long): List<StreakFreeze>

    @Query("SELECT * FROM streak_freezes WHERE identityId = :identityId")
    suspend fun getForIdentity(identityId: Long): List<StreakFreeze>

    @Query("SELECT EXISTS(SELECT 1 FROM streak_freezes WHERE habitId = :habitId AND date = :date)")
    suspend fun habitFrozenOn(habitId: Long, date: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM streak_freezes WHERE identityId = :identityId AND date = :date)")
    suspend fun identityFrozenOn(identityId: Long, date: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(freeze: StreakFreeze): Long
}
