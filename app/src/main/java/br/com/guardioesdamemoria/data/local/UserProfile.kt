package br.com.guardioesdamemoria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val school: String,
    val isTeacher: Boolean,
    val pin: String? = null,
    val lastUsed: Long = System.currentTimeMillis()
)
