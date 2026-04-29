package br.com.guardioesdamemoria.domain.model

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val points: Int,
    val dateEarned: Long = System.currentTimeMillis()
)
