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
        cleanupBrokenModels()
    }

    private fun cleanupBrokenModels() {
        // Any model < 100MB is likely a failed/truncated import for an LLM
        val brokenFiles = modelsDir.listFiles { file -> 
            file.isFile && (file.name.endsWith(".litertlm") || file.name.endsWith(".bin")) && file.length() < 100 * 1024 * 1024 
        } ?: emptyArray()
        
        brokenFiles.forEach { it.delete() }
    }

    private fun migrateLegacyModel() {
        val legacyFile = File(context.filesDir, "model.litertlm")
        if (legacyFile.exists() && legacyFile.length() > 100 * 1024 * 1024) {
            try {
                val targetFile = File(modelsDir, "gemma-2b-general.litertlm")
                if (!targetFile.exists()) legacyFile.renameTo(targetFile) else legacyFile.delete()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun getModels(): List<ModelProfile> {
        val files = modelsDir.listFiles { _, name -> name.endsWith(".litertlm") || name.endsWith(".bin") } ?: emptyArray()
        return files.filter { it.length() >= 100 * 1024 * 1024 }.map { file ->
            ModelProfile(
                name = file.name,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                tag = getTagForFile(file.name)
            )
        }
    }

    private fun getTagForFile(fileName: String): ModelTag {
        val tagStr = prefs.getString("tag_$fileName", null)
        if (tagStr != null) {
            return try { ModelTag.valueOf(tagStr) } catch (e: Exception) { ModelTag.GENERAL }
        }
        return when {
            fileName.contains("coder", ignoreCase = true) || fileName.contains("code", ignoreCase = true) -> ModelTag.CODER
            fileName.contains("rag", ignoreCase = true) || fileName.contains("doc", ignoreCase = true) -> ModelTag.RAG_READER
            else -> ModelTag.GENERAL
        }
    }

    fun getModelByTag(tag: ModelTag): ModelProfile? {
        val allModels = getModels()
        return allModels.find { it.tag == tag } ?: allModels.find { it.tag == ModelTag.GENERAL } ?: allModels.firstOrNull()
    }

    fun setModelTag(fileName: String, tag: ModelTag) {
        prefs.edit().putString("tag_$fileName", tag.name).apply()
    }

    fun importModel(inputStream: InputStream, originalFileName: String, expectedSize: Long): Result<ModelProfile> {
        var destinationFile: File? = null
        return try {
            val baseName = sanitizeFileName(originalFileName)
            
            // Deduplication: Check for exact identity (Name + Size)
            val existing = File(modelsDir, baseName)
            if (existing.exists() && existing.length() == expectedSize) {
                return Result.success(ModelProfile(baseName, existing.absolutePath, expectedSize, getTagForFile(baseName)))
            }

            // If a file with this name exists but size is different/wrong, get a unique name
            destinationFile = if (existing.exists()) getUniqueFile(baseName) else existing
            
            var bytesCopied: Long = 0
            inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        bytes = input.read(buffer)
                    }
                }
            }

            // INTEGRITY CHECK: Did we get exactly what we expected?
            if (expectedSize > 0 && bytesCopied != expectedSize) {
                destinationFile.delete()
                return Result.failure(Exception("Transfer incomplete: expected $expectedSize, got $bytesCopied"))
            }

            val fileName = destinationFile.name
            setModelTag(fileName, getTagForFile(fileName))

            Result.success(ModelProfile(fileName, destinationFile.absolutePath, bytesCopied, getTagForFile(fileName)))
        } catch (e: Exception) {
            destinationFile?.let { if (it.exists()) it.delete() }
            Result.failure(e)
        }
    }

    private fun getUniqueFile(fileName: String): File {
        val nameWithoutExt = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
        val ext = if (fileName.contains(".")) "." + fileName.substringAfterLast(".") else ""
        val cleanBase = nameWithoutExt.replace(Regex("_\\d+$"), "")
        
        var count = 1
        var file = File(modelsDir, "${cleanBase}_$count$ext")
        while (file.exists()) {
            file = File(modelsDir, "${cleanBase}_$count$ext")
            count++
        }
        return file
    }

    fun deleteModel(fileName: String): Boolean {
        val file = File(modelsDir, fileName)
        if (file.exists()) {
            val deleted = file.delete()
            if (deleted) prefs.edit().remove("tag_$fileName").apply()
            return deleted
        }
        return false
    }

    private fun sanitizeFileName(fileName: String): String {
        var clean = fileName
        // Strip Android URI suffixes from the display name before doing anything else
        if (clean.contains(".litertlm")) clean = clean.substringBefore(".litertlm") + ".litertlm"
        else if (clean.contains(".bin")) clean = clean.substringBefore(".bin") + ".bin"

        clean = clean.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        if (!clean.endsWith(".litertlm") && !clean.endsWith(".bin")) clean += ".litertlm"
        return clean
    }
}
