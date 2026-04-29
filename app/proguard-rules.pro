# Regras padrão do ProGuard para Guardiões da Memória

# Manter classes do Compose
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView { *; }
-keep class androidx.compose.runtime.Recomposer { *; }

# Regras para o Room (Banco de Dados)
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Manter modelos de dados para evitar problemas na serialização
-keep class br.com.guardioesdamemoria.domain.model.** { *; }
-keep class br.com.guardioesdamemoria.data.local.** { *; }

# Otimizações de desempenho
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
