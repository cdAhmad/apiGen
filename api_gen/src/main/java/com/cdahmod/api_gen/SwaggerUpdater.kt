package com.cdahmod.api_gen

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileWriter
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.iterator

class SwaggerUpdater(private val config: Map<String, Any>) {
    private val swaggerApiUrl: String = config.getOrDefault("swaggerapiurl", "").toString()
    private val apiGenDir: String = ".api_gen"
    private val downloadedFile: String = "$apiGenDir/default_OpenAPI.json"
    private val logDir: String = "$apiGenDir/logs"
    private val logFile: String = "$logDir/swagger_update.log"
    private val md5File: String = "$logDir/swagger_md5.txt"
    private val oldFile: String = "$logDir/swagger_old.json"

    init {
        // 确保 .api_gen 目录和日志目录存在
        File(apiGenDir).mkdirs()
        File(logDir).mkdirs()
    }

    fun downloadSwaggerJson(): Boolean {
        try {
            println("Downloading swagger json file from $swaggerApiUrl...")
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(swaggerApiUrl)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                File(downloadedFile).writeText(responseBody)
                println("Download completed, saved to $downloadedFile")
                return true
            }
        } catch (e: Exception) {
            println("Download failed: ${e.message}")
            return false
        }
    }

    fun calculateMd5(filePath: String): String {
        val md = MessageDigest.getInstance("MD5")
        val file = File(filePath)
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return BigInteger(1, md.digest()).toString(16).padStart(32, '0')
    }

    fun compareSwagger(oldFile: String, newFile: String): Map<String, List<String>> {
        val gson = Gson()
        val oldSwagger = gson.fromJson(File(oldFile).readText(), JsonObject::class.java)
        val newSwagger = gson.fromJson(File(newFile).readText(), JsonObject::class.java)

        val changes = mutableMapOf(
            "added_paths" to mutableListOf<String>(),
            "removed_paths" to mutableListOf<String>(),
            "modified_paths" to mutableListOf<String>()
        )

        val oldPaths = oldSwagger.getAsJsonObject("paths")?.asMap() ?: emptyMap()
        val newPaths = newSwagger.getAsJsonObject("paths")?.asMap() ?: emptyMap()

        // 检查新增和删除的 paths
        for ((path, value) in newPaths) {
            if (!oldPaths.containsKey(path)) {
                val pathObject = value as JsonObject
                for (method in pathObject.keySet()) {
                    val operation = pathObject.getAsJsonObject(method)
                    val summary = operation.get("summary")?.asString ?: "无描述"
                    changes["added_paths"]?.add("${method.uppercase()} $path - $summary")
                }
            }
        }

        for ((path, value) in oldPaths) {
            if (!newPaths.containsKey(path)) {
                val pathObject = value as JsonObject
                for (method in pathObject.keySet()) {
                    val operation = pathObject.getAsJsonObject(method)
                    val summary = operation.get("summary")?.asString ?: "无描述"
                    changes["removed_paths"]?.add("${method.uppercase()} $path - $summary")
                }
            }
        }

        // 检查修改的 paths
        for ((path, value) in oldPaths) {
            if (newPaths.containsKey(path)) {
                val oldPathObject = value as JsonObject
                val newPathObject = newPaths[path] as JsonObject

                for (method in oldPathObject.keySet()) {
                    if (newPathObject.has(method)) {
                        val oldOperation = oldPathObject.getAsJsonObject(method)
                        val newOperation = newPathObject.getAsJsonObject(method)

                        val oldParams = oldOperation.getAsJsonArray("parameters")?.toString() ?: "[]"
                        val newParams = newOperation.getAsJsonArray("parameters")?.toString() ?: "[]"

                        val oldResponses = oldOperation.getAsJsonObject("responses")?.toString() ?: "{}"
                        val newResponses = newOperation.getAsJsonObject("responses")?.toString() ?: "{}"

                        if (oldParams != newParams || oldResponses != newResponses) {
                            val summary = newOperation.get("summary")?.asString ?: "无描述"
                            changes["modified_paths"]?.add("${method.uppercase()} $path - $summary")
                        }
                    }
                }
            }
        }

        return changes
    }

    fun run(): Boolean {
        // Download swagger json file (if swaggerapiurl is not empty)
        val downloadSuccess = if (swaggerApiUrl.isNotEmpty()) {
            downloadSwaggerJson()
        } else {
            println("swaggerapiurl is empty, using local file: $downloadedFile")
            true
        }

        // Calculate MD5 value
        val currentMd5 = calculateMd5(downloadedFile)
        println("Current swagger json MD5 value: $currentMd5")

        // Read previous MD5 value
        var previousMd5: String? = null
        val md5FileObj = File(md5File)
        if (md5FileObj.exists()) {
            previousMd5 = md5FileObj.readText().trim()
            println("Previous swagger json MD5 value: $previousMd5")
        }

        // Compare MD5 values
        if (previousMd5 == currentMd5) {
            println("swagger json file has not changed, skipping subsequent execution")
            // Record log
            FileWriter(logFile, true).use { writer ->
                writer.write("[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}] swagger json file has not changed, MD5: $currentMd5\n")
            }
            return false
        } else {
            // On first run, print and record all paths and descriptions
            if (previousMd5 == null) {
                println("First run, printing all API information...")
                val gson = Gson()
                val swagger = gson.fromJson(File(downloadedFile).readText(), JsonObject::class.java)
                val paths = swagger.getAsJsonObject("paths")?.asMap() ?: emptyMap()
                val allPaths = mutableListOf<String>()
                for ((path, value) in paths) {
                    val pathObject = value as JsonObject
                    for (method in pathObject.keySet()) {
                        val operation = pathObject.getAsJsonObject(method)
                        val summary = operation.get("summary")?.asString ?: "No description"
                        allPaths.add("${method.uppercase()} $path - $summary")
                    }
                }

                // Print all API information
                println("All APIs:")
                allPaths.forEach { println("  - $it") }

                // Record to log
                FileWriter(logFile, true).use { writer ->
                    writer.write("[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}] First run, all APIs:\n")
                    allPaths.forEach { writer.write("  - $it\n") }
                }

                // On first run, only record all APIs, don't mark as added
                val changes = mapOf(
                    "added_paths" to emptyList<String>(),
                    "removed_paths" to emptyList<String>(),
                    "modified_paths" to emptyList<String>()
                )

                // Save current swagger json file as baseline for next comparison
                val downloadedFileObj = File(downloadedFile)
                if (downloadedFileObj.exists()) {
                    downloadedFileObj.copyTo(File(oldFile), overwrite = true)
                }
            } else {
                // Compare differences (compare before saving new file)
                val changes = try {
                    compareSwagger(oldFile, downloadedFile)
                } catch (e: Exception) {
                    println("Error comparing differences: ${e.message}")
                    mapOf(
                        "added_paths" to emptyList<String>(),
                        "removed_paths" to emptyList<String>(),
                        "modified_paths" to emptyList<String>()
                    )
                }

                // Save current swagger json file as baseline for next comparison
                val downloadedFileObj = File(downloadedFile)
                if (downloadedFileObj.exists()) {
                    downloadedFileObj.copyTo(File(oldFile), overwrite = true)
                }

                // Record change information to log
                FileWriter(logFile, true).use { writer ->
                    if (downloadSuccess) {
                        writer.write("[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}] swagger json file updated, MD5: $currentMd5\n")
                    } else {
                        writer.write("[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}] swagger json file updated (local file), MD5: $currentMd5\n")
                    }

                    changes["added_paths"]?.takeIf { it.isNotEmpty() }?.let {
                        writer.write("Added APIs:\n")
                        it.forEach { path -> writer.write("  - $path\n") }
                    }

                    changes["removed_paths"]?.takeIf { it.isNotEmpty() }?.let {
                        writer.write("Removed APIs:\n")
                        it.forEach { path -> writer.write("  - $path\n") }
                    }

                    changes["modified_paths"]?.takeIf { it.isNotEmpty() }?.let {
                        writer.write("Modified APIs (parameter or response changes):\n")
                        it.forEach { path -> writer.write("  - $path\n") }
                    }
                }

                // Print change information
                println("swagger json file has been updated, continuing with subsequent steps")
                changes["added_paths"]?.takeIf { it.isNotEmpty() }?.let {
                    println("Added APIs:")
                    it.forEach { path -> println("  - $path") }
                }

                changes["removed_paths"]?.takeIf { it.isNotEmpty() }?.let {
                    println("Removed APIs:")
                    it.forEach { path -> println("  - $path") }
                }

                changes["modified_paths"]?.takeIf { it.isNotEmpty() }?.let {
                    println("Modified APIs (parameter or response changes):")
                    it.forEach { path -> println("  - $path") }
                }
            }

            // Update MD5 value at the end of all tasks
            md5FileObj.writeText(currentMd5)

            return true
        }
    }

    private fun JsonObject.asMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        for (entry in entrySet()) {
            map[entry.key] = entry.value
        }
        return map
    }
}
