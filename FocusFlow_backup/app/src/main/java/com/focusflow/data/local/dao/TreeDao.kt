package com.focusflow.data.local.dao

import androidx.room.*
import com.focusflow.data.local.entity.TreeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTree(tree: TreeEntity): Long

    @Update
    suspend fun updateTree(tree: TreeEntity)

    @Delete
    suspend fun deleteTree(tree: TreeEntity)

    @Query("SELECT * FROM trees WHERE id = :id")
    suspend fun getTreeById(id: Long): TreeEntity?

    @Query("SELECT * FROM trees ORDER BY plantedDateMs DESC")
    fun getAllTrees(): Flow<List<TreeEntity>>

    @Query("SELECT * FROM trees WHERE growthStage = 'MATURE' ORDER BY plantedDateMs DESC")
    fun getFullyGrownTrees(): Flow<List<TreeEntity>>

    @Query("SELECT * FROM trees WHERE sessionId = :sessionId")
    suspend fun getTreeForSession(sessionId: Long): TreeEntity?

    @Query("SELECT COUNT(*) FROM trees WHERE growthStage = 'MATURE'")
    fun getFullyGrownTreeCount(): Flow<Int>

    @Query("SELECT * FROM trees WHERE plantedDateMs >= :startOfDay AND plantedDateMs < :endOfDay ORDER BY plantedDateMs DESC")
    fun getTreesForDay(startOfDay: Long, endOfDay: Long): Flow<List<TreeEntity>>
}
