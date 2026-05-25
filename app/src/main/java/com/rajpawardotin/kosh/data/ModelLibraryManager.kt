package com.rajpawardotin.kosh.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

enum class ModelTag { GENERAL, CODER, RAG_READER }

data class ModelProfile(
    val name: String,
    val filePath: String,
    val sizeBytes: Long,
    val tag: ModelTag
)

class ModelLibraryManager(private val context: Context) {
    private val modelsDir = File(context.filesDir, "models")
    private val prefs = context.getSharedPreferences("model_library_prefs", Context.MODE_PRIVATE)

    init {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        migrateLegacyModel()
    }

    private fun migrateLegacyModel() {
        val legacyFile = File(context.filesDir, "model.litertlm")
        if (legacyFile.exists() && legacyFile.length() > 10 * 1024 * 1024) {
            try {
                val targetFile = File(modelsDir, "gemma-2b-general.litertlm")
                if (!targetFile.exists()) {
                    legacyFile.renameTo(targetFile)
                } else {
                    legacyFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getModels(): List<ModelProfile> {
        val files = modelsDir.listFiles { _, name -> name.endsWith(".litertlm") || name.endsWith(".bin") } ?: emptyArray()
        return files.map { file ->
            val tagStr = prefs.getString("tag_${file.name}", ModelTag.GENERAL.name) ?: ModelTag.GENERAL.name
            val tag = try { ModelTag.valueOf(tagStr) } catch (e: Exception) { ModelTag.GENERAL }
            ModelProfile(
                name = file.name,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                tag = tag
            )
        }
    }

    fun getModelByTag(tag: ModelTag): ModelProfile? {
        val allModels = getModels()
        val matched = allModels.find { it.tag == tag }
        return matched ?: allModels.find { it.tag == ModelTag.GENERAL } ?: allModels.firstOrNull()
    }

    fun setModelTag(fileName: String, tag: ModelTag) {
        prefs.edit().putString("tag_$fileName", tag.name).apply()
    }

    fun importModel(inputStream: InputStream, originalFileName: String): Result<ModelProfile> {
        return try {
            val fileName = sanitizeFileName(originalFileName)
            val destinationFile = File(modelsDir, fileName)
            
            inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            val defaultTag = when {
                fileName.contains("coder", ignoreCase = true) || fileName.contains("code", ignoreCase = true) -> ModelTag.CODER
                fileName.contains("rag", ignoreCase = true) || fileName.contains("doc", ignoreCase = true) -> ModelTag.RAG_READER
                else -> ModelTag.GENERAL
            }
            setModelTag(fileName, defaultTag)

            Result.success(
                ModelProfile(
                    name = fileName,
                    filePath = destinationFile.absolutePath,
                    sizeBytes = destinationFile.length(),
                    tag = defaultTag
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteModel(fileName: String): Boolean {
        val file = File(modelsDir, fileName)
        if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                prefs.edit().remove("tag_$fileName").apply()
            }
            return deleted
        }
        return false
    }

    private fun sanitizeFileName(fileName: String): String {
        val clean = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return if (clean.endsWith(".litertlm") || clean.endsWith(".bin")) {
            clean
        } else {
            "$clean.litertlm"
        }
    }
}
