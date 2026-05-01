package br.com.guardioesdamemoria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_reactions")
data class MemoryReactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoryId: Long,
    val userId: Long,
    val reaction: String, // "Conectado", "Surpreso", "Inspirado"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_reports")
data class MemoryReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoryId: Long,
    val userId: Long,
    val reason: String = "Conteúdo inadequado",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pendente"
)
