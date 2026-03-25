package com.example.weeboo_habittrackerfriend.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.weeboo_habittrackerfriend.data.local.entity.WeebooEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface WeebooDao {
    @Query("SELECT * FROM weeboos LIMIT 1")
    fun getWeeboo(): Flow<WeebooEntity?>
    @Query("SELECT * FROM weeboos LIMIT 1")
    suspend fun getWeebooSync(): WeebooEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeboo(weeboo: WeebooEntity): Long
    @Update
    suspend fun updateWeeboo(weeboo: WeebooEntity)
}
