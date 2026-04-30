package br.com.guardioesdamemoria.domain.model

data class Memory(
    val id: String,
    val title: String,
    val description: String,
    val category: String = "Geral",
    val year: String = "",
    val authorName: String = "",
    val authorAge: String = "",
    val latitude: Double,
    val longitude: Double,
    val triggerRadiusMeters: Double = 50.0,
    val imageUrl: String? = null,
    val imageSource: String = "",
    val audioUrl: String? = null,
    val isApproved: Boolean = true
)
