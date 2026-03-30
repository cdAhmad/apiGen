package com.cdahmod.api_gen

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.openapitools.codegen.OpenAPIGenerator
import java.io.File

fun main(args: Array<String>) {

    // 加载配置文件
    val configFile = if (args.isNotEmpty()) {
        File(args[0])
    } else {
        File(System.getProperty("user.dir"), "config.json")
    }
    if (!configFile.exists()) {
        println("config.json file does not exist, generating config file...")
        // Generate random salt value
        fun generateRandomSalt(length: Int = 32): String {
            val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (1..length)
                .map { chars.random() }
                .joinToString("")
        }

        // Generate default config
        val defaultConfig = mutableMapOf(
            "salt" to "swagger-kotlin-codegen-salt-${generateRandomSalt()}",
            "customTag" to "Default",
            "outputDir" to "generated-code",
            "package" to "com.temp.net",
            "apiPackage" to "com.temp.net.api",
            "modelPackage" to "com.temp.net.bean",
            "sourceFolder" to "src/main/kotlin",
            "swaggerapiurl" to "",
            "baseResponseName" to "BaseResponse"
        )

        // Save config file
        val gson = Gson().newBuilder().setPrettyPrinting().create()
        val jsonString = gson.toJson(defaultConfig)
        configFile.writeText(jsonString)
        println("Generated default config file: ${configFile.name}")
        println("Please configure config.json file and run the script again")
        return
    }

    // 读取配置文件
    val config =
        Gson().fromJson(configFile.readText(), JsonObject::class.java)

    // 从配置文件中读取配置项
    val outputDir = if (config.has("outputDir")) config.get("outputDir").asString else "generated-code"
    val packageName = if (config.has("package")) config.get("package").asString else "com.temp.net"
    val modelPackage = if (config.has("modelPackage")) config.get("modelPackage").asString else "${packageName}.bean"
    val apiPackage = if (config.has("apiPackage")) config.get("apiPackage").asString else "${packageName}.api"
    val sourceFolder = if (config.has("sourceFolder")) config.get("sourceFolder").asString else "src/main/kotlin"
    val swaggerApiUrl = if (config.has("swaggerapiurl")) config.get("swaggerapiurl").asString else ""
    val baseResponseName = if (config.has("baseResponseName")) config.get("baseResponseName").asString else "BaseResponse"

    println("Configuration read from config file:")
    println("output_dir: $outputDir")
    println("package: $packageName")
    println("model_package: $modelPackage")
    println("api_package: $apiPackage")
    println("source_folder: $sourceFolder")
    println("swaggerapiurl: $swaggerApiUrl")
    println("base_response_name: $baseResponseName")

    // 检查 swaggerApiUrl 是否有效
    if (swaggerApiUrl.isBlank()) {
        println("Error: swaggerApiUrl is blank")
        return
    }

    // 运行 swagger 更新流程
    val updater = SwaggerUpdater(mapOf(
        "swaggerapiurl" to swaggerApiUrl
    ))
    if (!updater.run()) {
        return
    }

    // Run clean_swagger_script
    println("\nRunning clean_swagger_script...")
    val cleanSwaggerScript = CleanSwaggerScript()
    try {
        cleanSwaggerScript.cleanSwagger(".api_gen/default_OpenAPI.json", ".api_gen/temp.json", configFile.absolutePath)
        println("clean_swagger_script executed successfully")
    } catch (e: Exception) {
        println("clean_swagger_script execution failed: ${e.message}")
        return
    }

    // Check if temp.json was generated
    val tempJson = File(".api_gen/temp.json")
    if (!tempJson.exists()) {
        println("Error: temp.json file was not generated, please check if clean_swagger_script ran successfully")
        return
    }

    // Use the OpenAPI generator from dependencies to generate Kotlin code
    println("\nRunning openapi-generator...")
    try {
        // Create temporary directory for templates
        val tempFile = File.createTempFile(".api_gen_build", null)
        tempFile.delete()
        tempFile.mkdirs()
        val templateDir = File(tempFile, "templates")
        templateDir.mkdirs()
        
        // Copy api.mustache from resources to temporary directory
        val templateStream = Class.forName("com.cdahmod.api_gen.MainKt").classLoader.getResourceAsStream("templates/api.mustache")
        if (templateStream != null) {
            val templateFile = File(templateDir, "api.mustache")
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
        } else {
            throw Exception("api.mustache template not found in resources")
        }
        
        val args = arrayOf(
            "generate",
            "-i",
            ".api_gen/temp.json",
            "-g",
            "kotlin",
            "-o",
            outputDir,
            "--template-dir",
            tempFile.absolutePath,
            "--global-property",
            "apis,models,modelDocs",
            "--additional-properties",
            "generateApiTests=false,generateModelTests=false,performBeanValidation=false,useResponseAsReturnType=false,serializableModel=true,nullableReturnType=true,dateLibrary=string,useCoroutines=true,library=jvm-retrofit2,generateAliasAsModel=true,serializationLibrary=kotlinx_serialization,interfaceOnly=false,apiPackage=$apiPackage,modelPackage=$modelPackage,sourceFolder=$sourceFolder,baseResponseName=$baseResponseName"
        )
        println("Execution parameters: ${args.joinToString(" ")}")
        OpenAPIGenerator.main(args)
        println("openapi-generator executed successfully")
        
        // Clean up temporary directory
        tempFile.deleteRecursively()
    } catch (e: Exception) {
        println("openapi-generator execution failed: ${e.message}")
        e.printStackTrace()
    }

    // Check if generation was successful
    val outputDirFile = File(outputDir)
    if (!outputDirFile.exists()) {
        println("Error: Generation failed, output directory was not created")
        return
    }

    // Call create_base_response
    println("\nRunning create_base_response...")
    try {
        val createBaseResponse = CreateBaseResponse()
        createBaseResponse.createBaseResponse(modelPackage, "$outputDir/$sourceFolder", baseResponseName)
        println("create_base_response executed successfully")
    } catch (e: Exception) {
        println("create_base_response execution failed: ${e.message}")
    }

    println("\nAll operations completed!")
}
