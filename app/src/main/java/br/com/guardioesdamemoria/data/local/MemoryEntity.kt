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
    val authorAge: String = "",
    val latitude: Double,
    val longitude: Double,
    val triggerRadiusMeters: Double = 50.0,
    val imageUrl: String? = null,
    val imageSource: String = "",
    val audioUrl: String? = null,
    val isApproved: Boolean = false,
    val createdAt: Long
)
