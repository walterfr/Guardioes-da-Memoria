package br.com.guardioesdamemoria.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: MemoryReactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: MemoryReportEntity)

    @Query("SELECT * FROM memory_reactions WHERE memoryId = :memoryId")
    fun getReactionsForMemory(memoryId: Long): Flow<List<MemoryReactionEntity>>

    @Query("SELECT * FROM memory_reports")
    fun getAllReports(): Flow<List<MemoryReportEntity>>
}
