# api_gen_py

将 Swagger API 文档转换为 Kotlin 代码（suspend + Retrofit2 + kotlinx.serialization）

纯 Python 标准库实现，零外部依赖。

## 安装

```bash
cd api_gen_py
pip install -e .
```

安装后可用 `api-gen-py` 命令，或直接用 Python 运行：

```bash
python3 -m api_gen_py --swaggerApiUrl <URL> --salt <SALT>
```

## 快速开始

```bash
python3 -m api_gen_py \
  --swaggerApiUrl "https://your-server.com/v2/api-docs" \
  --salt "my-salt-123" \
  --outputDir "./generated-code" \
  --package "com.example.api"
```

## 参数说明

| 参数 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `--swaggerApiUrl` | 是 | - | Swagger/OpenAPI JSON URL |
| `--salt` | 是 | - | SHA-256 混淆盐值 |
| `--outputDir` | 否 | `generated-code` | 输出目录 |
| `--package` | 否 | `com.example.api` | 代码根包名 |
| `--modelPackage` | 否 | `{package}.bean` | 数据模型包名 |
| `--apiPackage` | 否 | `{package}.api` | API 接口包名 |
| `--sourceFolder` | 否 | `src/main/kotlin` | 源码子目录 |
| `--baseResponseName` | 否 | `BaseResponse` | 响应包装类名 |
| `--apiName` | 否 | `Default` | 单接口模式下的接口名 |
| `--obfuscateOperationId` | 否 | `true` | 是否混淆 operationId |
| `--library` | 否 | `jvm-retrofit2` | HTTP 客户端库 |
| `--apiGenDir` | 否 | `{outputDir}/api_gen` | 缓存/日志工作目录 |
| `--splitByTag` | 否 | `false` | 按 Swagger tag 拆分为多个接口 |
| `--disableModelMapping` | 否 | `false` | 禁用模型名混淆 |
| `--modelNameMap` | 否 | - | 固定模型名映射 JSON 文件 |
| `--exportModelNameMap` | 否 | `{apiGenDir}/model_name_mapping.json` | 导出映射文件路径 |
| `--exportMappingOnly` | 否 | `false` | 仅导出映射不生成代码 |

## 生成结果

```
generated-code/
├── src/main/kotlin/com/example/api/
│   ├── bean/
│   │   ├── BaseResponse.kt          ← 泛型响应包装器
│   │   └── Xxx.kt × N               ← @Serializable 数据类
│   └── api/
│       └── DefaultApi.kt            ← Retrofit2 接口 (suspend)
│
└── api_gen/                          ← 缓存目录（自动管理）
    ├── generate.sh                   ← 完整执行命令（含 salt，可直接 ./generate.sh 重建）
    ├── model_name_mapping.json       ← 模型名映射（纳入版本控制，增量复用）
    ├── swagger_update.log            ← 全量变更日志（首行记录 salt）
    ├── logs/
    │   ├── default_OpenAPI.json      ← 当前 Swagger 快照
    │   ├── swagger_old.json          ← 上次 Swagger（用于 diff）
    │   ├── swagger_md5.txt           ← MD5 去重缓存
    │   ├── changelog_<ts>.md         ← 每次变更的独立报告
    │   ├── common_headers.json       ← 公共 header 列表
    │   └── temp.json                 ← 清洗后的临时文件
    └── history/
        ├── swagger_<ts>.json         ← 历史 Swagger 快照
        └── code_<ts>/                ← 变更前生成代码的完整备份
```

### BaseResponse.kt

```kotlin
@Serializable
data class BaseResponse<T>(
    @SerialName("code") val code: kotlin.Int,
    @SerialName("msg") val msg: kotlin.String,
    @SerialName("data") val `data`: T? = null,
)
```

### 模型示例

```kotlin
/**
 * 原始名: 注册对象请求传参
 * 被以下接口引用:
 *   POST /hCN1Z... - 提交进件数据
 *   POST /pqvRI... - 注销用户账号
 */
@Serializable
data class XyzAbc(
    // authCode 验证码
    val cRWllbT: kotlin.String,
    // phone 手机号
    val qT8_jY: kotlin.String
)
```

每个字段上方有 `// 原始字段名 中文描述`，类上方有 `原始名` 和 `被以下接口引用`。

### API 接口示例

```kotlin
/**
 * adjust实时回调结果
 * GET /fGaY2wT/t3Ej/...
 *
 * Responses:
 *   200 - success
 *   401 - Unauthorized
 *   403 - Forbidden
 *   404 - Not Found
 */
@Get("/fGaY2wT/t3Ej/...")
suspend fun qjLnZZQIIaHt(
    @Query("param1") param1: kotlin.String? = null,
    @Query("param2") param2: kotlin.String? = null
): BaseResponse<SomeModel>

// 文件上传
@Multipart
@Post("/upload")
suspend fun upload(
    @Part file: okhttp3.MultipartBody.Part
): BaseResponse<UploadResult>
```

### createHeaders()

自动识别所有接口中出现的公共 header 参数，生成独立 `ApiHeaders` object：

```kotlin
object ApiHeaders {
    @JvmStatic
    fun createHeaders(
        appVersion: kotlin.String,
        clientType: kotlin.String,
        acqChannel: kotlin.String,
        adid: kotlin.String,
        advId: kotlin.String? = null,
        token: kotlin.String? = null,
        deviceId: kotlin.String? = null
    ): kotlin.collections.Map<kotlin.String, kotlin.String> {
        return buildMap {
            put("nNM6CMAs9MZ4", appVersion)
            put("sTA987j", clientType)
            put("sV7Dx6zhvbUUxPYqyIbm", acqChannel)
            put("uSXDfqmOWF", adid)
            if (advId != null) put("gYaSxn1okg", advId)
            if (token != null) put("mQ61OyI46PS", token)
            if (deviceId != null) put("vIICL7r3PxioXRbg", deviceId)
        }
    }
}
```

### --splitByTag 拆分

```bash
python3 -m api_gen_py ... --splitByTag true
```

按 Swagger 原始 tag 拆分为多个接口：

```kotlin
/**
 * App Order Controller
 */
interface XNRbKNntIGlpsPKuLXH {
    suspend fun apYvqyEQSwjl(...): BaseResponse<...>
}

/**
 * App Common Controller
 */
interface SzSniwIVvjxntnywiK {
    suspend fun dGkmsqsBtkAE(): BaseResponse<...>
}

// ... 9 个 interface 对应 9 个 tag
```

## salt 保存

每次运行自动在 `api_gen/generate.sh` 保存完整执行命令（含 salt），可直接重新运行：

```bash
./api_gen/generate.sh
```

`salt` 同时记录在 `swagger_update.log` 首行。确保团队成员使用相同 salt，否则混淆名会不一致。

## Swagger 变更检测

每次运行时计算 Swagger JSON 的 MD5，与缓存对比：

```bash
# 无变更 → 跳过
swagger json file has not changed, skipping subsequent execution

# 有变更 → 生成详细变更报告
swagger json file has been updated, continuing with subsequent steps

Added APIs:
  + POST /new/path - 新接口描述

Removed APIs:
  - GET /old/path - 已删除接口

Modified APIs:
  GET /changed/path - 某接口
      + 新增 query 参数: pageSize
      - 删除参数: oldParam
      * 必填变更 token: False → True
      * 响应 200 返回字段变更:
      [data]  + 新增返回字段: newField
      [data]  - 删除返回字段: id (用户标识)
```

变更时自动备份：
- `history/swagger_<时间戳>.json` — 旧 Swagger 快照
- `history/code_<时间戳>/` — 旧生成代码完整备份
- `logs/changelog_<时间戳>.md` — 独立变更报告（Markdown）

## 增量模型映射

首次运行后自动导出 `model_name_mapping.json`。后续传入可保持模型名不变：

```bash
# 步骤 1：审核映射
python3 -m api_gen_py ... --exportMappingOnly true
# → 编辑 model_name_mapping.json，修改不满意的映射名

# 步骤 2：用审核后的映射生成
python3 -m api_gen_py ... --modelNameMap "./api_gen/model_name_mapping.json"
```

如果 Swagger 新增了模型，工具会提示：

```
⚠ New model names detected (need confirmation):
  NewModel → XyzAbc
Please review and re-run with --modelNameMap ...
```

## 依赖

零外部依赖，仅需 Python 3.10+ 标准库。
