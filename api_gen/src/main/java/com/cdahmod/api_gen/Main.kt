package com.cdahmod.api_gen

import org.openapitools.codegen.OpenAPIGenerator
import java.io.File

fun main(args: Array<String>) {

    // 解析命令行参数
    val params = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        if (args[i].startsWith("--")) {
            val key = args[i].substring(2)
            if (i + 1 < args.size && !args[i + 1].startsWith("--")) {
                params[key] = args[i + 1]
                i += 2
            } else {
                params[key] = ""
                i += 1
            }
        } else {
            i += 1
        }
    }

    // 从命令行参数中读取配置项
    val outputDir = params.getOrDefault("outputDir", "generated-code")
    val packageName = params.getOrDefault("package", "com.temp.net")
    val modelPackage = params.getOrDefault("modelPackage", "${packageName}.bean")
    val apiPackage = params.getOrDefault("apiPackage", "${packageName}.api")
    val sourceFolder = params.getOrDefault("sourceFolder", "src/main/kotlin")
    val swaggerApiUrl = params.getOrDefault("swaggerApiUrl", "")
    val baseResponseName = params.getOrDefault("baseResponseName", "BaseResponse")
    val apiName = params.getOrDefault("apiName", "Default")
    val obfuscateOperationId = params.getOrDefault("obfuscateOperationId", "true").toBoolean()
    val apiGenDir = params.getOrDefault("apiGenDir", ".api_gen")
    
    // salt 为必传参数，为其添加前缀
    val saltInput = params["salt"]
    if (saltInput.isNullOrBlank()) {
        println("Error: salt is required")
        return
    }
    val salt = "swagger-kotlin-codegen-salt-$saltInput"

    println("Configuration from command line arguments:")
    println("output_dir: $outputDir")
    println("package: $packageName")
    println("model_package: $modelPackage")
    println("api_package: $apiPackage")
    println("source_folder: $sourceFolder")
    println("swaggerapiurl: $swaggerApiUrl")
    println("base_response_name: $baseResponseName")
    println("salt: $salt")
    println("apiName: $apiName")
    println("obfuscateOperationId: $obfuscateOperationId")
    println("apiGenDir: $apiGenDir")

    // 检查 swaggerApiUrl 是否有效
    if (swaggerApiUrl.isBlank()) {
        println("Error: swaggerApiUrl is blank")
        return
    }

    // 运行 swagger 更新流程
    val updater = SwaggerUpdater(
        mapOf(
            "swaggerapiurl" to swaggerApiUrl,
            "apiGenDir" to apiGenDir
        )
    )
    if (!updater.run()) {
        return
    }

    // Run clean_swagger_script
    println("\nRunning clean_swagger_script...")
    val cleanSwaggerScript = CleanSwaggerScript(salt, apiName, obfuscateOperationId)
    try {
        cleanSwaggerScript.cleanSwagger("$apiGenDir/logs/default_OpenAPI.json", "$apiGenDir/logs/temp.json")
        println("clean_swagger_script executed successfully")
    } catch (e: Exception) {
        println("clean_swagger_script execution failed: ${e.message}")
        return
    }

    // Check if temp.json was generated
    val tempJson = File("$apiGenDir/logs/temp.json")
    if (!tempJson.exists()) {
        println("Error: temp.json file was not generated, please check if clean_swagger_script ran successfully")
        return
    }

    // Use the OpenAPI generator from dependencies to generate Kotlin code
    println("\nRunning openapi-generator...")
    try {
        // Use apiGenDir/logs directory for templates
        val apiGenDirFile = File(apiGenDir)
        if (!apiGenDirFile.exists()) {
            apiGenDirFile.mkdirs()
        }
        // OpenAPI Generator Kotlin generator expects templates in 'kotlin' subdirectory


        // Copy api.mustache from resources to template directory
        println("Template directory: ${apiGenDirFile.absolutePath}")
        val templateFile = File(apiGenDir, "logs/api.mustache")
        println("Checking if template file exists: ${templateFile.absolutePath}")
        if (!templateFile.exists()) {
            println("Template file does not exist, copying from resources...")
            val templateStream =
                Class.forName("com.cdahmod.api_gen.MainKt").classLoader.getResourceAsStream("templates/api.mustache")
            if (templateStream != null) {
                val outputStream = templateFile.outputStream()
                try {
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (templateStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                } finally {
                    templateStream.close()
                    outputStream.close()
                }
                println("Copied api.mustache template to: ${templateFile.absolutePath}")
            } else {
                throw Exception("api.mustache template not found in resources")
            }
        } else {
            println("api.mustache template already exists at: ${templateFile.absolutePath}, skipping copy")
        }
        //将 模板文件 中 BaseResponse 字符串 替换为 baseResponseName
        val baseResponseRegex = Regex("BaseResponse")
        val templateContent = templateFile.readText().replace(baseResponseRegex, baseResponseName)
        templateFile.writeText(templateContent)
        val args = arrayOf(
            "generate",
            "-i",
            "$apiGenDir/logs/temp.json",
            "-g",
            "kotlin",
            "-o",
            outputDir,
            "--template-dir",
            "$apiGenDir/logs",
            "--global-property",
            "apis,models,modelDocs,modelTests=false,apiTests=false",
            "--additional-properties",
            "generateApiTests=false,generateModelTests=false,performBeanValidation=false,useResponseAsReturnType=false,serializableModel=true,nullableReturnType=true,dateLibrary=string,useCoroutines=true,library=jvm-retrofit2,generateAliasAsModel=true,serializationLibrary=kotlinx_serialization,interfaceOnly=false,apiPackage=$apiPackage,modelPackage=$modelPackage,sourceFolder=$sourceFolder"
        )
        println("Execution parameters: ${args.joinToString(" ")}")
        OpenAPIGenerator.main(args)
        println("openapi-generator executed successfully")

    } catch (e: Exception) {
        println("openapi-generator execution failed: ${e.message}")
        e.printStackTrace()
    }

    // Check if generation was successful


    // Call create_base_response
    println("\nRunning create_base_response...")
    try {
        val createBaseResponse = CreateBaseResponse()
        val effectiveOutputDir = if (outputDir.isBlank()) "." else outputDir
        createBaseResponse.createBaseResponse(
            modelPackage,
            "$effectiveOutputDir/$sourceFolder",
            baseResponseName
        )
        println("create_base_response executed successfully")
    } catch (e: Exception) {
        println("create_base_response execution failed: ${e.message}")
    }

    println("\nAll operations completed!")
}
