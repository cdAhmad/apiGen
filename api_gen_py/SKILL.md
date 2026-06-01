---
name: api-gen-py
description: 将 Swagger/OpenAPI 文档转换为 Kotlin 代码（suspend + Retrofit2 + kotlinx.serialization）。纯 Python 实现。触发：(1) 从 Swagger URL 或本地 JSON 文件生成 Kotlin API 客户端，(2) 接口变更后重新生成，(3) 模型名映射审核与增量更新，(4) 按 tag 拆分接口。
---

# api_gen_py

纯 Python 实现的 Swagger → Kotlin 代码生成器。零第三方依赖。

## Agent 执行流程

1. **确认必填参数**：`--swaggerApiUrl`（Swagger JSON URL 或本地文件路径）和 `--salt`（缺失则询问用户）
2. **询问可选参数**：逐项确认以下配置，用户无特殊要求则使用默认值——
   - `--outputDir`：输出目录（默认 `generated-code`）
   - `--package`：根包名（默认 `com.example.api`）
   - `--modelPackage`：模型包名（默认 `{package}.bean`）
   - `--apiPackage`：API 包名（默认 `{package}.api`）
   - `--splitByTag`：按 tag 拆分接口（默认 `false`）
   - `--exportMappingOnly`：仅导出映射不生成代码（默认 `false`）
   - `--modelNameMap`：固定映射 JSON 文件路径（增量用）
   - `--disableModelMapping`：禁用模型名混淆（默认 `false`）
   - `--baseResponseName`：响应包装类名（默认 `BaseResponse`）
   - `--obfuscateOperationId`：混淆 operationId（默认 `true`）
   - `--apiName`：API 接口名称（默认 `Default`）
   - `--library`：HTTP 客户端库（默认 `jvm-retrofit2`）
3. **进入 skill 目录**：`cd api_gen_py`
4. **运行生成**：`python3 scripts/main.py ...`
5. **报告结果**：文件数量、API 方法数、公共 header 数

## 命令行参考

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `--swaggerApiUrl` | **(必填)** | Swagger JSON URL 或本地文件路径 |
| `--salt` | **(必填)** | 混淆盐值（选定后不可更换） |
| `--outputDir` | `generated-code` | 输出目录 |
| `--package` | `com.example.api` | 根包名 |
| `--modelPackage` | `{package}.bean` | 模型包名 |
| `--apiPackage` | `{package}.api` | API 包名 |
| `--splitByTag` | `false` | 按 tag 拆分多接口 |
| `--exportMappingOnly` | `false` | 仅导出映射，不生成代码 |
| `--modelNameMap` | - | 固定映射 JSON（增量用） |
| `--disableModelMapping` | `false` | 禁用模型名混淆 |
| `--baseResponseName` | `BaseResponse` | 响应包装类名 |
| `--obfuscateOperationId` | `true` | 混淆 operationId |

## 典型场景

### 首次生成（URL）
```bash
python3 scripts/main.py \
  --swaggerApiUrl "https://xxx/v2/api-docs" \
  --salt "project-unique-salt" \
  --outputDir "./api"
```

### 首次生成（本地文件）
```bash
python3 scripts/main.py \
  --swaggerApiUrl "./swagger.json" \
  --salt "project-unique-salt" \
  --outputDir "./api"
```

### 审核模型名后生成
```bash
# step 1: 导出映射
python3 scripts/main.py ... --exportMappingOnly true
# → 编辑 {outputDir}/api_gen/model_name_mapping.json

# step 2: 用固定映射生成
python3 scripts/main.py ... --modelNameMap "./api/api_gen/model_name_mapping.json"
```

### 按业务模块拆分
```bash
python3 scripts/main.py ... --splitByTag true
```

## 生成产物

```
<outputDir>/
├── src/main/kotlin/<package>/
│   ├── bean/
│   │   ├── BaseResponse.kt     ← @Serializable data class BaseResponse<T>
│   │   └── *.kt                ← 含原始名+使用接口+字段描述的注释
│   └── api/
│       └── DefaultApi.kt       ← suspend fun + KDoc(描述+路径+响应码)
└── api_gen/
    ├── generate.sh              ← 完整执行命令（含 salt，可重新运行）
    ├── model_name_mapping.json  ← 模型名映射（纳入版本控制）
    ├── swagger_update.log       ← 全量变更日志
    └── logs/
        ├── changelog_*.md       ← 每次变更独立报告
        ├── common_headers.json  ← 公共 header 列表
        └── swagger_md5.txt      ← MD5 缓存
```

## 关键行为

- **salt 保存**：每次运行自动保存 `api_gen/generate.sh`，含完整命令和 salt，避免丢失
- **MD5 去重**：Swagger 未变更时跳过生成
- **变更检测**：Swagger 更新时输出字段级 diff（新增/删除参数、返回字段、响应码变更）
- **自动备份**：变更时将旧代码备份到 `api_gen/history/code_<ts>/`
- **模型名稳定**：首次运行导出映射，后续用 `--modelNameMap` 保持模型名不变
- **公共 Header**：自动识别跨接口公共 header，生成 `ApiHeaders.createHeaders()` 方法

## 生成代码特征

- `suspend fun` + Retrofit2 注解（`@Get/@Post/@Multipart`）
- `@Serializable data class` + `@SerialName` 字段
- 每字段上方 `// 原始名 中文描述` 注释
- 每模型类 KDoc 含原始名和使用它的接口列表
- 每 API 方法 KDoc 含描述 + HTTP 路径 + 所有响应码
- 文件上传生成 `@Part okhttp3.MultipartBody.Part`

## 注意事项

- **salt 一旦确定不可更换**，否则所有混淆名变化，现有引用全部失效
- `model_name_mapping.json` 和 `generate.sh` 应纳入版本控制
- 需要 Python 3.10+，无需 pip install，直接运行脚本即可
