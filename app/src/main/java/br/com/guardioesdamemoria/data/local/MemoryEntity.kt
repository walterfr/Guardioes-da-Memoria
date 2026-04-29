package br.com.guardioesdamemoria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val year: String,
    val authorName: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val createdAt: Long
)
