package br.com.guardioesdamemoria.util

import android.content.Context
import androidx.core.content.FileProvider
import br.com.guardioesdamemoria.data.local.MemoryEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DatabaseExporter(private val context: Context) {

    fun export(memories: List<MemoryEntity>): android.net.Uri {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipFile = File(exportDir, "guardioes_memoria_$timestamp.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            zip.writestr(
                name = "README.txt",
                content = buildReadme(memories.size, timestamp)
            )
            zip.writestr(
                name = "memories.json",
                content = buildJson(memories)
            )
            memories.forEach { memory ->
                memory.imageUrl?.let { path ->
                    zip.writeFileIfExists(path, "images", memory.id)
                }
                memory.audioUrl?.let { path ->
                    zip.writeFileIfExists(path, "audio", memory.id)
                }
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )
    }

    private fun buildReadme(total: Int, timestamp: String): String {
        return """
            Guardioes da Memoria - Exportacao local
            Gerado em: $timestamp
            Total de memorias: $total

            Conteudo:
            - memories.json: dados completos do acervo local.
            - images/: imagens historicas anexadas, quando existirem.
            - audio/: audios dos relatos, quando existirem.

            Observacao:
            Este arquivo pode conter voz, nomes, locais e imagens de moradores.
            Compartilhe apenas com pessoas autorizadas pelo projeto.
        """.trimIndent()
    }

    private fun buildJson(memories: List<MemoryEntity>): String {
        return memories.joinToString(
            prefix = "{\n  \"version\": 1,\n  \"exportedAt\": ${System.currentTimeMillis()},\n  \"memories\": [\n",
            postfix = "\n  ]\n}",
            separator = ",\n"
        ) { memory ->
            """
                {
                  "id": ${memory.id},
                  "title": "${memory.title.escapeJson()}",
                  "description": "${memory.description.escapeJson()}",
                  "category": "${memory.category.escapeJson()}",
                  "year": "${memory.year.escapeJson()}",
                  "authorName": "${memory.authorName.escapeJson()}",
                  "authorAge": "${memory.authorAge.escapeJson()}",
                  "latitude": ${memory.latitude},
                  "longitude": ${memory.longitude},
                  "triggerRadiusMeters": ${memory.triggerRadiusMeters},
                  "imageSource": "${memory.imageSource.escapeJson()}",
                  "imageFile": ${memory.imageUrl?.let { "\"${mediaName(it, "images", memory.id).escapeJson()}\"" } ?: "null"},
                  "audioFile": ${memory.audioUrl?.let { "\"${mediaName(it, "audio", memory.id).escapeJson()}\"" } ?: "null"},
                  "isApproved": ${memory.isApproved},
                  "createdAt": ${memory.createdAt}
                }
            """.trimIndent().prependIndent("    ")
        }
    }

    private fun ZipOutputStream.writestr(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.writeFileIfExists(path: String, folder: String, memoryId: Long) {
        val file = File(path)
        if (!file.exists() || !file.isFile) return

        putNextEntry(ZipEntry(mediaName(path, folder, memoryId)))
        FileInputStream(file).use { input ->
            input.copyTo(this)
        }
        closeEntry()
    }

    private fun mediaName(path: String, folder: String, memoryId: Long): String {
        val file = File(path)
        val extension = file.extension.ifBlank { "bin" }
        return "$folder/memory_${memoryId}.${extension}"
    }

    private fun String.escapeJson(): String {
        val builder = StringBuilder(length)
        forEach { char ->
            when (char) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }
}
