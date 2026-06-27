package com.inkhabits.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inkhabits.data.entity.EconomyState
import kotlinx.coroutines.flow.Flow

@Dao
interface EconomyDao {
    @Query("SELECT * FROM economy WHERE id = 1")
    fun observe(): Flow<EconomyState?>

    @Query("SELECT * FROM economy WHERE id = 1")
    suspend fun get(): EconomyState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: EconomyState)
}
