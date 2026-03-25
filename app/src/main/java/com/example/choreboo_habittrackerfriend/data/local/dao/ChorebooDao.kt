package com.example.choreboo_habittrackerfriend.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.choreboo_habittrackerfriend.data.local.entity.ChorebooEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface ChorebooDao {
    @Query("SELECT * FROM choreboos LIMIT 1")
    fun getChoreboo(): Flow<ChorebooEntity?>
    @Query("SELECT * FROM choreboos LIMIT 1")
    suspend fun getChorebooSync(): ChorebooEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChoreboo(choreboo: ChorebooEntity): Long
    @Update
    suspend fun updateChoreboo(choreboo: ChorebooEntity)
}
