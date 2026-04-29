package br.com.guardioesdamemoria.domain.model

data class Memory(
    val id: String,
    val title: String,
    val description: String,
    val category: String = "Geral",
    val year: String = "",
    val authorName: String = "",
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val audioUrl: String? = null
)
