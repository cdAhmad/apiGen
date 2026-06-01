---
name: api-gen-py
description: 将 Swagger/OpenAPI 文档转换为 Kotlin 代码（suspend + Retrofit2 + kotlinx.serialization）。纯 Python 实现，零依赖。触发场景：(1) 从 Swagger URL 生成 Kotlin API 客户端，(2) Swagger 接口变更后重新生成，(3) 模型名映射审核与增量更新，(4) 按 tag 拆分接口文件。
---

# api_gen_py

将 Swagger API 文档转换为 Kotlin 代码的工具，生成 `suspend fun` + Retrofit2 + kotlinx.serialization 代码。

纯 Python 3.10+ 标准库实现，无需安装额外依赖。

## 快速开始

调用本技能时按以下步骤执行：

1. **确认 Swagger URL 和 salt**（必填）
2. **确认输出目录和包名**（可自动推导）
3. **执行生成**
4. **展示生成结果**

最小命令：

```bash
python3 -m api_gen_py \
  --swaggerApiUrl "https://xxx/v2/api-docs" \
  --salt "my-salt" \
  --outputDir "./output" \
  --package "com.example.api"
```

## 命令行选项

| 选项 | 说明 | 默认值 | 必填 |
|------|------|--------|------|
| `--swaggerApiUrl` | Swagger JSON URL | - | **是** |
| `--salt` | SHA-256 混淆盐值 | - | **是** |
| `--outputDir` | 输出目录 | `generated-code` | 否 |
| `--package` | 根包名 | `com.example.api` | 否 |
| `--modelPackage` | 模型包名 | `{package}.bean` | 否 |
| `--apiPackage` | API 接口包名 | `{package}.api` | 否 |
| `--sourceFolder` | 源码子目录 | `src/main/kotlin` | 否 |
| `--baseResponseName` | 响应包装类名 | `BaseResponse` | 否 |
| `--apiName` | 单接口模式下的接口名 | `Default` | 否 |
| `--obfuscateOperationId` | 混淆 operationId 和模型名 | `true` | 否 |
| `--library` | HTTP 客户端库 | `jvm-retrofit2` | 否 |
| `--apiGenDir` | 缓存/日志目录 | `{outputDir}/api_gen` | 否 |
| `--splitByTag` | 按 Swagger tag 拆分接口 | `false` | 否 |
| `--disableModelMapping` | 禁用模型名映射 | `false` | 否 |
| `--modelNameMap` | 固定模型名映射 JSON | - | 否 |
| `--exportModelNameMap` | 导出映射路径 | `{apiGenDir}/model_name_mapping.json` | 否 |
| `--exportMappingOnly` | 仅导出映射不生成代码 | `false` | 否 |

## 生成代码结构

```
<outputDir>/
├── src/main/kotlin/<package>/
│   ├── bean/
│   │   ├── BaseResponse.kt    ← @Serializable 泛型响应包装器
│   │   └── *.kt               ← @Serializable data class
│   └── api/
│       └── DefaultApi.kt      ← Retrofit2 interface (suspend fun)
└── api_gen/                    ← 缓存/日志（自动管理）
    ├── logs/
    │   ├── changelog_*.md      ← 每次变更报告
    │   ├── common_headers.json ← 公共 header 列表
    │   └── swagger_md5.txt     ← MD5 去重
    ├── history/                ← 变更时自动备份旧代码
    └── model_name_mapping.json ← 模型名映射
```

## 功能特性

- **Swagger 清洗**：移除 header 参数、剥离 code/msg/data 响应包装器、SHA-256 混淆模型名和 operationId
- **公共 Header 识别**：自动识别所有接口中的公共 header，生成 `ApiHeaders.createHeaders()` 方法
- **变更检测**：MD5 对比 + 字段级 diff（新增/删除/修改参数、返回字段、响应码）
- **变更备份**：Swagger 更新时自动备份旧 Swagger 快照 + 旧生成代码到 `history/`
- **变更报告**：控制台打印 + `swagger_update.log` + `changelog_<ts>.md` 三份记录
- **模型注释**：类注释含原始名 + 引用该模型的接口列表，字段注释含原始名 + 中文描述
- **API 注释**：KDoc 含接口描述 + HTTP 路径 + 所有响应码
- **增量映射**：`--exportMappingOnly` 先导出审核 → `--modelNameMap` 固定映射重新生成
- **Tag 拆分**：`--splitByTag true` 按 Swagger tag 拆分为多个 interface（含描述）
- **文件上传**：`@Multipart` + `@Part okhttp3.MultipartBody.Part` 正确生成

## 典型工作流

### 首次生成

```bash
python3 -m api_gen_py \
  --swaggerApiUrl "https://xxx/v2/api-docs" \
  --salt "my-salt" \
  --outputDir "./api-output" \
  --package "com.example.api"
```

### 增量更新（模型名稳定）

```bash
# 步骤 1：仅导出映射，审核模型名
python3 -m api_gen_py \
  --swaggerApiUrl "https://xxx/v2/api-docs" \
  --salt "my-salt" \
  --outputDir "./api-output" \
  --exportMappingOnly true

# 步骤 2：用审核后的映射生成
python3 -m api_gen_py \
  --swaggerApiUrl "https://xxx/v2/api-docs" \
  --salt "my-salt" \
  --outputDir "./api-output" \
  --modelNameMap "./api-output/api_gen/model_name_mapping.json"
```

### 按 Tag 拆分

```bash
python3 -m api_gen_py \
  --swaggerApiUrl "https://xxx/v2/api-docs" \
  --salt "my-salt" \
  --outputDir "./api-output" \
  --splitByTag true
```

## 生成代码示例

```kotlin
// API 接口
/**
 * 客服信息查询
 * GET /qKeUfx.../qgqa5BfXiP
 *
 * Responses:
 *   200 - success
 *   401 - Unauthorized
 */
@Get("/qKeUfx.../qgqa5BfXiP")
suspend fun oHuWBjAAkUeXtvYMKQ(): BaseResponse<RrZVSbsFucnrlmWBMB>

// 模型
/**
 * 原始名: App客服信息信息列表对象
 * 被以下接口引用:
 *   GET /qKeUfx... - 客服信息查询
 */
@Serializable
data class RrZVSbsFucnrlmWBMB(
    // appCustomerServiceInfoResps 客服信息列表
    val p8_iG9aWsBLwPkc: kotlin.collections.List<OcrWggTruuTFHQFfUy>? = null
)

// 公共 Header
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

## 注意事项

- 盐值 `salt` 决定混淆结果，不同项目使用不同盐值，同一项目始终保持不变
- 模型名映射文件 `model_name_mapping.json` 应纳入版本控制，确保团队成员生成一致的代码
- Swagger 无变更时不会重复生成（MD5 去重）
- 运行需要 Python 3.10+，无需安装任何第三方库

## 技术细节

- **清洗步骤**：移除 header 参数 → 修复 query 类型 → 处理 tags → 混淆 operationId → 移除 originalRef → 剥离响应包装器 → 过滤非标 HTTP 码 → 混淆定义名 → 更新引用 → 导出映射
- **哈希算法**：SHA-256(salt + original) → 取前 4 字节定长(12-20) → Base-52 编码 → PascalCase
- **变更检测**：`_deep_resolve` 递归解析 `$ref` 和 `originalRef`，完整展开 schema 后再对比
