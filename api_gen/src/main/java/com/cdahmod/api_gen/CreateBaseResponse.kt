package com.cdahmod.api_gen

import java.io.File

class CreateBaseResponse {
    fun createBaseResponse(modelPackage: String, outputDir: String, baseResponseName: String = "BaseResponse") {
        // 将包路径转换为文件路径
        val packagePath = modelPackage.replace('.', '/')
        val filePath = "$outputDir/$packagePath/${baseResponseName}.kt"

        // 创建目录结构
        val directory = File(filePath).parentFile
        directory.mkdirs()

        // BaseResponse.kt 文件内容
        val content = """package $modelPackage

import kotlinx.serialization.Serializable as KSerializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual
import java.io.Serializable

/**
 *
 *
 * @param code 错误状态
 * @param msg 错误消息
 * @param `data` 通用对象，用于泛型推断
 */
@KSerializable
data class $baseResponseName<T> (

    /* 错误状态 */
    @SerialName(value = "code")
    val code: kotlin.Int,

    /* 错误消息 */
    @SerialName(value = "msg")
    val msg: kotlin.String,

    /* 通用对象，用于泛型推断 */
    @Contextual @SerialName(value = "data")
    val `data`: T? = null

) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 123
    }


}
"""

        // 写入文件
        File(filePath).writeText(content)

        println("Successfully created ${baseResponseName}.kt file: $filePath")
    }
}
