package com.cdahmod.api_gen

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.math.BigInteger
import java.util.*
import kotlin.collections.iterator

class CleanSwaggerScript(private val salt: String, private val apiName: String = "Default", private val obfuscateOperationId: Boolean = true) {
    fun generateFieldName(original: String): String {
        if (salt.isBlank()) {
            throw IllegalArgumentException("salt must not be blank")
        }

        val alphabet = ('A'..'Z') + ('a'..'z')
        val alphabetSize = alphabet.size

        // 1. 加盐哈希 (SHA-256)
        val inputStr = original + salt
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(inputStr.toByteArray())

        // 2. 计算 target_length：使用 hash_bytes 的前 4 字节计算一个 12-20 之间的值
        val lengthSeed = BigInteger(1, hashBytes.copyOfRange(0, 4)).toLong()
        val targetLength = 12 + (lengthSeed % 9).toInt()

        // 3. 转为正的大整数
        val bigInt = BigInteger(1, hashBytes)

        // 4. 逐位取模生成字母
        val sb = StringBuilder()
        var value = bigInt

        for (i in 0 until targetLength) {
            if (value == BigInteger.ZERO) {
                sb.append('A')
            } else {
                val index = value.mod(BigInteger.valueOf(alphabetSize.toLong())).toInt()
                sb.append(alphabet[index])
                value = value.divide(BigInteger.valueOf(alphabetSize.toLong()))
            }
        }

        // 5. 强制 PascalCase：首字母大写（其余保持原样）
        val result = sb.toString()
        return result.substring(0, 1).uppercase() + result.substring(1)
    }

    fun cleanSwagger(inputFile: String, outputFile: String) {

        // 加载swagger文件
        val gson = Gson()
        val swagger = gson.fromJson(File(inputFile).readText(), JsonObject::class.java)

        println("Starting to clean $inputFile")
        println("Using salt: $salt")

        // 1. 清洗paths中的header参数
        var headerCount = 0
        if (swagger.has("paths")) {
            val paths = swagger.getAsJsonObject("paths")
            for (entry in paths.entrySet()) {
                val path = entry.key
                val pathObject = entry.value as JsonObject
                for (methodEntry in pathObject.entrySet()) {
                    val method = methodEntry.key
                    val operation = methodEntry.value as JsonObject
                    if (operation.has("parameters")) {
                        val originalCount = operation.getAsJsonArray("parameters").size()
                        // 过滤掉in为header的参数
                        val filteredParams = JsonArray()
                        for (paramElement in operation.getAsJsonArray("parameters")) {
                            val param = paramElement as JsonObject
                            if (param.get("in")?.asString != "header") {
                                // 确保参数有name属性
                                if (!param.has("name")) {
                                    // 如果没有name属性，使用参数的in值作为默认name
                                    param.addProperty("name", param.get("in")?.asString ?: "param")
                                }
                                
                                // 处理query参数中type字段错误的情况
                                if (param.get("in")?.asString == "query" && param.has("type")) {
                                    val validTypes = setOf("string", "number", "integer", "boolean", "array", "object")
                                    val originalType = param.get("type").asString
                                    if (!validTypes.contains(originalType)) {
                                        // 将错误的type转换为string
                                        param.addProperty("type", "string")
                                        
                                        // 在description中追加转换错误信息及原始type
                                        val errorMessage = "注意：参数类型已从 $originalType 转换为 string（类型错误）"
                                        if (param.has("description")) {
                                            val currentDesc = param.get("description").asString
                                            param.addProperty("description", "$currentDesc\n$errorMessage")
                                        } else {
                                            param.addProperty("description", errorMessage)
                                        }
                                    }
                                }
                                
                                filteredParams.add(param)
                            }
                        }
                        operation.add("parameters", filteredParams)
                        headerCount += originalCount - operation.getAsJsonArray("parameters").size()
                    }
                }
            }
        }

        println("Removed $headerCount header parameters")

        // 1.1 处理tags，支持自定义tag值
        var updateTagCount = 0
        // 使用 apiName
        var targetTag: String? = apiName
        println("Using api name: $targetTag")

        if (swagger.has("paths") && targetTag != null) {
            val paths = swagger.getAsJsonObject("paths")
            for (entry in paths.entrySet()) {
                val path = entry.key
                val pathObject = entry.value as JsonObject
                for (methodEntry in pathObject.entrySet()) {
                    val method = methodEntry.key
                    val operation = methodEntry.value as JsonObject
                    // 将所有操作的tags设置为目标tag
                    val tagsArray = JsonArray()
                    tagsArray.add(targetTag)
                    operation.add("tags", tagsArray)
                    updateTagCount++
                }
            }
        }
        println("Updated tags for $updateTagCount operations, all set to: $targetTag")

        // 1.2 混淆 paths 中的 operationId 值
        var operationIdCount = 0
        if (swagger.has("paths")) {
            val paths = swagger.getAsJsonObject("paths")
            for (entry in paths.entrySet()) {
                val path = entry.key
                val pathObject = entry.value as JsonObject
                for (methodEntry in pathObject.entrySet()) {
                    val method = methodEntry.key
                    val operation = methodEntry.value as JsonObject
                    if (operation.has("operationId") && obfuscateOperationId) {
                    val originalOperationId = operation.get("operationId").asString
                    // 生成混淆后的 operationId
                    var hashedOperationId = generateFieldName(originalOperationId)
                    // 确保首字母是小写
                    hashedOperationId = hashedOperationId.substring(0, 1).lowercase() + hashedOperationId.substring(1)
                    // 更新 operationId
                    operation.addProperty("operationId", hashedOperationId)
                    operationIdCount++
                }
                }
            }
        }        
        if (obfuscateOperationId) {
            println("Obfuscated $operationIdCount operationId values")
        } else {
            println("Skipped operationId obfuscation")
        }

        // 2. 移除originalRef属性
        fun removeOriginalRef(obj: JsonElement) {
            if (obj is JsonObject) {
                obj.remove("originalRef")
                for (entry in obj.entrySet()) {
                    removeOriginalRef(entry.value)
                }
            } else if (obj is JsonArray) {
                for (element in obj) {
                    removeOriginalRef(element)
                }
            }
        }

        removeOriginalRef(swagger)
        println("Removed all originalRef properties")

        // 3. 处理包含 code、msg、data 的响应体模型
        val responseModelsToRemove = mutableMapOf<String, JsonObject>()
        val dataModelMapping = mutableMapOf<String, JsonElement>()

        // 遍历 paths，找到 responses 中指向的 model
        if (swagger.has("paths")) {
            val paths = swagger.getAsJsonObject("paths")
            for (entry in paths.entrySet()) {
                val path = entry.key
                val pathObject = entry.value as JsonObject
                for (methodEntry in pathObject.entrySet()) {
                    val method = methodEntry.key
                    val operation = methodEntry.value as JsonObject
                    if (operation.has("responses")) {
                        val responses = operation.getAsJsonObject("responses")
                        for (responseEntry in responses.entrySet()) {
                            val responseCode = responseEntry.key
                            val response = responseEntry.value as JsonObject
                            if (response.has("schema") && response.getAsJsonObject("schema").has("\$ref")) {
                                val ref = response.getAsJsonObject("schema").get("\$ref").asString
                                val modelName = ref.split('/').last()
                                
                                // 检查这个 model 是否包含 code、msg 字段
                                if (swagger.has("definitions")) {
                                    val definitions = swagger.getAsJsonObject("definitions")
                                    if (definitions.has(modelName)) {
                                        val model = definitions.getAsJsonObject(modelName)
                                        if (model.has("properties")) {
                                            val props = model.getAsJsonObject("properties")
                                            if (props.has("code") && props.has("msg")) {
                                                // 这是一个响应体模型，需要处理
                                                responseModelsToRemove[modelName] = model
                                                // 提取 data 字段的内容，如果没有则使用空对象
                                                val dataProp = if (props.has("data")) {
                                                    props.get("data")
                                                } else {
                                                    // 创建一个空的 object 类型
                                                    val emptyObject = JsonObject()
                                                    emptyObject.addProperty("type", "object")
                                                    emptyObject
                                                }
                                                // 记录映射关系：响应体模型 -> data 内容
                                                dataModelMapping[modelName] = dataProp
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 更新 paths 中的 response 引用
        if (swagger.has("paths") && dataModelMapping.isNotEmpty()) {
            val paths = swagger.getAsJsonObject("paths")
            for (entry in paths.entrySet()) {
                val path = entry.key
                val pathObject = entry.value as JsonObject
                for (methodEntry in pathObject.entrySet()) {
                    val method = methodEntry.key
                    val operation = methodEntry.value as JsonObject
                    if (operation.has("responses")) {
                        val responses = operation.getAsJsonObject("responses")
                        for (responseEntry in responses.entrySet()) {
                            val responseCode = responseEntry.key
                            val response = responseEntry.value as JsonObject
                            if (response.has("schema") && response.getAsJsonObject("schema").has("\$ref")) {
                                val ref = response.getAsJsonObject("schema").get("\$ref").asString
                                val modelName = ref.split('/').last()
                                if (dataModelMapping.containsKey(modelName)) {
                                    // 将 response 的 schema 替换为 data 内容
                                    response.add("schema", dataModelMapping[modelName])
                                }
                            }
                        }
                    }
                }
            }
        }

        // 从 definitions 中移除响应体模型
        if (responseModelsToRemove.isNotEmpty()) {
            val definitions = swagger.getAsJsonObject("definitions")
            for (modelName in responseModelsToRemove.keys) {
                definitions.remove(modelName)
            }
            println("Removed ${responseModelsToRemove.size} response body models containing code, msg, data")
            println("Updated paths to reference data content in responses")
        } else {
            println("No response body models containing code, msg, data found")
        }

        // 4. 过滤非标准 HTTP 响应代码（只保留 1xx-5xx）
        if (swagger.has("paths")) {
            val paths = swagger.getAsJsonObject("paths")
            for (entry in paths.entrySet()) {
                val path = entry.key
                val pathObject = entry.value as JsonObject
                for (methodEntry in pathObject.entrySet()) {
                    val method = methodEntry.key
                    val operation = methodEntry.value as JsonObject
                    if (operation.has("responses")) {
                        val responses = operation.getAsJsonObject("responses")
                        // 只保留标准 HTTP 响应代码
                        val standardResponses = JsonObject()
                        val nonStandardDescriptions = mutableListOf<String>()
                        for (responseEntry in responses.entrySet()) {
                            val code = responseEntry.key
                            val response = responseEntry.value as JsonObject
                            
                            // 如果 response 没有 schema，添加一个空的 object 作为兜底
                            if (!response.has("schema")) {
                                val emptyObjectSchema = JsonObject()
                                emptyObjectSchema.addProperty("type", "object")
                                response.add("schema", emptyObjectSchema)
                            }
                            
                            try {
                                val codeNum = code.toInt()
                                if (codeNum in 100..599) {
                                    standardResponses.add(code, response)
                                } else {
                                    // 收集非标准 HTTP 响应代码的 description
                                    if (response.has("description")) {
                                        nonStandardDescriptions.add("Code $code: ${response.get("description").asString}")
                                    }
                                }
                            } catch (e: NumberFormatException) {
                                // 保留非数字的响应代码（如 default）
                                standardResponses.add(code, response)
                            }
                        }
                        // 更新响应
                        operation.add("responses", standardResponses)
                        // 将非标准 HTTP 响应代码的 description 追加到 operation 的 description 中
                        if (nonStandardDescriptions.isNotEmpty()) {
                            val description = if (operation.has("description")) {
                                operation.get("description").asString + "\n\n非标准响应代码：\n" + nonStandardDescriptions.joinToString("\n")
                            } else {
                                "非标准响应代码：\n" + nonStandardDescriptions.joinToString("\n")
                            }
                            operation.addProperty("description", description)
                        }
                    }
                }
            }
            println("Filtered non-standard HTTP response codes and appended non-standard response code descriptions to operation descriptions")
        }

        // 5. 混淆definitions中的model名称
        val refMapping = mutableMapOf<String, String>()
        if (swagger.has("definitions")) {
            val definitions = swagger.getAsJsonObject("definitions")
            val newDefinitions = JsonObject()

            // 先创建映射关系
            for (entry in definitions.entrySet()) {
                val modelName = entry.key
                var hashedName = generateFieldName(modelName)
                refMapping[modelName] = hashedName
                newDefinitions.add(hashedName, entry.value)
            }

            // 更新swagger中的definitions
            swagger.add("definitions", newDefinitions)

            // 6. 更新所有对模型的引用
            // 更新paths中的引用
            if (swagger.has("paths")) {
                val paths = swagger.getAsJsonObject("paths")
                for (entry in paths.entrySet()) {
                    val path = entry.key
                    val pathObject = entry.value as JsonObject
                    for (methodEntry in pathObject.entrySet()) {
                        val method = methodEntry.key
                        val operation = methodEntry.value as JsonObject
                        // 更新parameters中的引用
                        if (operation.has("parameters")) {
                            for (paramElement in operation.getAsJsonArray("parameters")) {
                                val param = paramElement as JsonObject
                                if (param.has("schema") && param.getAsJsonObject("schema").has("\$ref")) {
                                    val ref = param.getAsJsonObject("schema").get("\$ref").asString
                                    val modelName = ref.split('/').last()
                                    if (refMapping.containsKey(modelName)) {
                                        param.getAsJsonObject("schema").addProperty("\$ref", "#/definitions/${refMapping[modelName]}")
                                    }
                                }
                            }
                        }
                        // 更新responses中的引用
                        if (operation.has("responses")) {
                            val responses = operation.getAsJsonObject("responses")
                            for (responseEntry in responses.entrySet()) {
                                val response = responseEntry.value as JsonObject
                                if (response.has("schema")) {
                                    val schema = response.getAsJsonObject("schema")
                                    if (schema.has("\$ref")) {
                                        val ref = schema.get("\$ref").asString
                                        val modelName = ref.split('/').last()
                                        if (refMapping.containsKey(modelName)) {
                                            schema.addProperty("\$ref", "#/definitions/${refMapping[modelName]}")
                                        }
                                    } else if (schema.has("items") && schema.getAsJsonObject("items").has("\$ref")) {
                                        // 处理数组类型的引用
                                        val items = schema.getAsJsonObject("items")
                                        val ref = items.get("\$ref").asString
                                        val modelName = ref.split('/').last()
                                        if (refMapping.containsKey(modelName)) {
                                            items.addProperty("\$ref", "#/definitions/${refMapping[modelName]}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 更新definitions内部的引用
            if (swagger.has("definitions")) {
                val updatedDefinitions = swagger.getAsJsonObject("definitions")
                for (entry in updatedDefinitions.entrySet()) {
                    val modelName = entry.key
                    val model = entry.value as JsonObject
                    if (model.has("properties")) {
                        val properties = model.getAsJsonObject("properties")
                        for (propEntry in properties.entrySet()) {
                            val prop = propEntry.value
                            if (prop is JsonObject && prop.has("\$ref")) {
                                val ref = prop.get("\$ref").asString
                                val refModelName = ref.split('/').last()
                                if (refMapping.containsKey(refModelName)) {
                                    prop.addProperty("\$ref", "#/definitions/${refMapping[refModelName]}")
                                }
                            }
                        }
                    }
                }
            }
        }

        println("Obfuscated ${refMapping.size} model names")
        if (refMapping.isNotEmpty()) {
            println("\nModel name mappings:")
            for ((oldName, newName) in refMapping) {
                try {
                    println("$oldName -> $newName")
                } catch (e: Exception) {
                    // Handle encoding errors, skip model names with special characters
                }
            }
        }

        // 保存清洗后的swagger文件
        try {
            val gsonBuilder = Gson().newBuilder().setPrettyPrinting()
            val jsonString = gsonBuilder.create().toJson(swagger)
            File(outputFile).writeText(jsonString)
            println("\nCleaning completed, result saved to $outputFile")
        } catch (e: Exception) {
            println("\nFailed to save file: ${e.message}")
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
