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
    private val apiGenDir: String = config.getOrDefault("apiGenDir", ".api_gen").toString()
    private val logDir: String = "$apiGenDir/logs"
    private val historyDir: String = "$apiGenDir/history"
    private val downloadedFile: String = "$logDir/default_OpenAPI.json" // 移动到logs目录
    private val logFile: String = "${apiGenDir}/swagger_update.log" // 移动到apiGen目录外
    private val md5File: String = "$logDir/swagger_md5.txt"
    private val oldFile: String = "$logDir/swagger_old.json"

    init {
        // 确保 .api_gen 目录和日志目录存在
        File(apiGenDir).mkdirs()
        File(logDir).mkdirs()
        File(historyDir).mkdirs()
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

                        val paramsChanged = oldParams != newParams
                        val responsesChanged = oldResponses != newResponses
                        if (paramsChanged || responsesChanged) {
                            val summary = newOperation.get("summary")?.asString ?: "无描述"
                            val changeType = when {
                                paramsChanged && responsesChanged -> "[参数+响应变更]"
                                paramsChanged -> "[参数变更]"
                                responsesChanged -> "[响应变更]"
                                else -> ""
                            }

                            // 参数详情对比
                            val paramDetails = mutableListOf<String>()
                            if (paramsChanged) {
                                val oldParamsArray = oldOperation.getAsJsonArray("parameters")
                                val newParamsArray = newOperation.getAsJsonArray("parameters")
                                val oldParamMap = oldParamsArray?.mapNotNull {
                                    it as? JsonObject
                                }?.associateBy { it.get("name")?.asString ?: "" } ?: emptyMap()
                                val newParamMap = newParamsArray?.mapNotNull {
                                    it as? JsonObject
                                }?.associateBy { it.get("name")?.asString ?: "" } ?: emptyMap()

                                val oldParamNames = oldParamMap.keys.filter { it.isNotEmpty() }.toSet()
                                val newParamNames = newParamMap.keys.filter { it.isNotEmpty() }.toSet()
                                val addedParams = newParamNames - oldParamNames
                                val removedParams = oldParamNames - newParamNames
                                if (addedParams.isNotEmpty()) paramDetails.add("新增参数: ${addedParams.joinToString(", ")}")
                                if (removedParams.isNotEmpty()) paramDetails.add("删除参数: ${removedParams.joinToString(", ")}")

                                // 同名参数属性变化
                                for (name in oldParamNames.intersect(newParamNames)) {
                                    val oldParam = oldParamMap[name]!!
                                    val newParam = newParamMap[name]!!
                                    val oldType = oldParam.get("type")?.asString
                                        ?: oldParam.getAsJsonObject("schema")?.get("type")?.asString ?: "unknown"
                                    val newType = newParam.get("type")?.asString
                                        ?: newParam.getAsJsonObject("schema")?.get("type")?.asString ?: "unknown"
                                    if (oldType != newType) {
                                        paramDetails.add("$name 类型变更: $oldType -> $newType")
                                    }
                                    val oldRequired = oldParam.get("required")?.asBoolean ?: false
                                    val newRequired = newParam.get("required")?.asBoolean ?: false
                                    if (oldRequired != newRequired) {
                                        paramDetails.add("$name 必填变更: ${if (oldRequired) "必填" else "可选"} -> ${if (newRequired) "必填" else "可选"}")
                                    }
                                    val oldIn = oldParam.get("in")?.asString ?: ""
                                    val newIn = newParam.get("in")?.asString ?: ""
                                    if (oldIn != newIn) {
                                        paramDetails.add("$name 位置变更: $oldIn -> $newIn")
                                    }
                                }
                            }

                            // 响应详情对比
                            val responseDetails = mutableListOf<String>()
                            if (responsesChanged) {
                                val oldResponsesObj = oldOperation.getAsJsonObject("responses")
                                val newResponsesObj = newOperation.getAsJsonObject("responses")
                                val oldResponseCodes = oldResponsesObj?.keySet() ?: emptySet()
                                val newResponseCodes = newResponsesObj?.keySet() ?: emptySet()
                                val addedResponses = newResponseCodes - oldResponseCodes
                                val removedResponses = oldResponseCodes - newResponseCodes
                                if (addedResponses.isNotEmpty()) responseDetails.add("新增响应码: ${addedResponses.joinToString(", ")}")
                                if (removedResponses.isNotEmpty()) responseDetails.add("删除响应码: ${removedResponses.joinToString(", ")}")

                                // 同响应码下 schema/响应体字段变化
                                for (code in oldResponseCodes.intersect(newResponseCodes)) {
                                    val oldResponse = oldResponsesObj?.getAsJsonObject(code)
                                    val newResponse = newResponsesObj?.getAsJsonObject(code)
                                    if (oldResponse.toString() != newResponse.toString()) {
                                        val parts = mutableListOf<String>()

                                        // description 变更
                                        val oldDesc = oldResponse?.get("description")?.asString ?: ""
                                        val newDesc = newResponse?.get("description")?.asString ?: ""
                                        if (oldDesc != newDesc) {
                                            parts.add("描述变更")
                                        }

                                        // schema 变更
                                        val oldSchema = oldResponse?.getAsJsonObject("schema")
                                        val newSchema = newResponse?.getAsJsonObject("schema")
                                        if (oldSchema?.has("\$ref") == true && newSchema?.has("\$ref") == true) {
                                            val oldRef = oldSchema.get("\$ref").asString
                                            val newRef = newSchema.get("\$ref").asString
                                            if (oldRef != newRef) {
                                                parts.add("引用变更: $oldRef -> $newRef")
                                            }
                                        } else if (oldSchema?.has("properties") == true && newSchema?.has("properties") == true) {
                                            val oldPropsObj = oldSchema.getAsJsonObject("properties")
                                            val newPropsObj = newSchema.getAsJsonObject("properties")
                                            val oldProps = oldPropsObj.keySet()
                                            val newProps = newPropsObj.keySet()
                                            val addedProps = newProps - oldProps
                                            val removedProps = oldProps - newProps
                                            if (addedProps.isNotEmpty()) parts.add("新增字段: ${addedProps.joinToString(", ")}")
                                            if (removedProps.isNotEmpty()) parts.add("删除字段: ${removedProps.joinToString(", ")}")

                                            // 同字段名 type 变化
                                            for (propName in oldProps.intersect(newProps)) {
                                                val oldProp = oldPropsObj.getAsJsonObject(propName)
                                                val newProp = newPropsObj.getAsJsonObject(propName)
                                                val oldType = oldProp?.get("type")?.asString ?: oldProp?.get("\$ref")?.asString ?: "unknown"
                                                val newType = newProp?.get("type")?.asString ?: newProp?.get("\$ref")?.asString ?: "unknown"
                                                if (oldType != newType) {
                                                    parts.add("$propName 类型变更: $oldType -> $newType")
                                                }
                                            }
                                        } else if (oldSchema.toString() != newSchema.toString()) {
                                            parts.add("响应体结构变更")
                                        }

                                        if (parts.isNotEmpty()) {
                                            responseDetails.add("$code ${parts.joinToString(", ")}")
                                        }
                                    }
                                }
                            }

                            val detailStr = (paramDetails + responseDetails).joinToString("; ")
                            val fullDesc = if (detailStr.isNotEmpty()) "$changeType ($detailStr)" else changeType
                            changes["modified_paths"]?.add("${method.uppercase()} $path - $fullDesc - $summary")
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
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                writer.write("\n")
                writer.write("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                writer.write("[$timestamp] swagger json file has not changed, MD5: $currentMd5\n")
                writer.write("Summary: +0 added, -0 removed, ~0 modified\n")
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
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                    val totalApis = allPaths.size
                    writer.write("\n")
                    writer.write("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                    writer.write("[$timestamp] First run, all APIs:\n")
                    writer.write("Summary: total $totalApis APIs registered as baseline\n")
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

                // Save historical version with timestamp
                val timestampForFile = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val historyFile = "$historyDir/swagger_${timestampForFile}.json"
                if (downloadedFileObj.exists()) {
                    downloadedFileObj.copyTo(File(historyFile), overwrite = true)
                    println("Historical swagger json saved to $historyFile")
                }

                // Record change information to log
                FileWriter(logFile, true).use { writer ->
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                    val totalAdded = changes["added_paths"]?.size ?: 0
                    val totalRemoved = changes["removed_paths"]?.size ?: 0
                    val totalModified = changes["modified_paths"]?.size ?: 0
                    val hasChanges = totalAdded > 0 || totalRemoved > 0 || totalModified > 0

                    writer.write("\n")
                    writer.write("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                    if (downloadSuccess) {
                        writer.write("[$timestamp] swagger json file updated, MD5: $currentMd5\n")
                    } else {
                        writer.write("[$timestamp] swagger json file updated (local file), MD5: $currentMd5\n")
                    }
                    writer.write("Summary: +$totalAdded added, -$totalRemoved removed, ~$totalModified modified\n")
                    if (!hasChanges) {
                        writer.write("No API changes detected (MD5 changed but content diff is empty)\n")
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
                        it.forEach { path ->
                            // 标记破坏性变更
                            val isBreaking = path.contains("删除参数") || path.contains("删除字段") ||
                                    path.contains("删除响应码") || path.contains("类型变更") ||
                                    path.contains("位置变更") || path.contains("必填变更: 可选 -> 必填")
                            val markedPath = if (isBreaking) "  - [破坏性] $path" else "  - $path"
                            writer.write("$markedPath\n")
                        }
                    }
                }

                // Print change information
                println("swagger json file has been updated, continuing with subsequent steps")
                val totalAdded = changes["added_paths"]?.size ?: 0
                val totalRemoved = changes["removed_paths"]?.size ?: 0
                val totalModified = changes["modified_paths"]?.size ?: 0
                println("Summary: +$totalAdded added, -$totalRemoved removed, ~$totalModified modified")

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
                    it.forEach { path ->
                        val isBreaking = path.contains("删除参数") || path.contains("删除字段") ||
                                path.contains("删除响应码") || path.contains("类型变更") ||
                                path.contains("位置变更") || path.contains("必填变更: 可选 -> 必填")
                        if (isBreaking) println("  - [破坏性] $path") else println("  - $path")
                    }
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
