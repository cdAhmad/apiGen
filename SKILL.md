---
name: "api-gen"
description: "将 Swagger API 文档转换为 Kotlin 代码（Retrofit 2 接口 + kotlinx.serialization 数据类）。当用户需要从 Swagger/OpenAPI 规范为 Android/Java 项目生成 Kotlin API 客户端代码时调用。"
---

# API Gen 技能

## 概述

API Gen 是一款将 Swagger API 文档转换为 Kotlin 代码的工具，专为使用 Retrofit 2 的 Android/Java 项目设计。它可以生成：
- API 服务接口
- 模型类
- 响应基类

## 快速开始

### 前置条件

- Java 11 或更高版本
- Swagger/OpenAPI JSON 端点（URL）

### 自动配置

调用本技能时，将按以下步骤执行：

1. **分析项目结构**，确定合适的配置
2. **询问 Swagger API URL 或文件路径**（必填）
3. **根据项目特征预测最优设置**
4. **展示建议配置**供你审阅
5. **请求确认**，确保配置正确
6. **执行生成任务**，仅在确认后运行

这种渐进式方法可确保你获得最适合项目的配置，同时全程掌控流程。

### 重要提示

Swagger API URL 或文件路径是 **必填项**，调用本技能时会提示你提供该信息。

## 使用方式

### 通过 JitPack 使用

#### Gradle
```groovy
repositories {
    maven {
        url 'https://www.jitpack.io'
    }
}
```

### 命令行选项

| 选项 | 说明 | 默认值 | 是否必填 |
|------|------|--------|----------|
| --outputDir | 生成代码的输出目录 | generated-code | 否 |
| --package | 基础包名 | com.temp.net | 否 |
| --modelPackage | 模型类包名 | {package}.bean | 否 |
| --apiPackage | API 服务接口包名 | {package}.api | 否 |
| --sourceFolder | 源文件夹名称 | src/main/kotlin | 否 |
| --swaggerApiUrl | Swagger API URL（支持 http/https 协议） | 无 | **是** |
| --baseResponseName | 响应基类名称 | BaseResponse | 否 |
| --salt | 混淆盐值（用于模型名与操作 ID 的哈希混淆） | 无 | **是** |
| --apiName | API 名称（用于统一设置所有接口的 tag） | Default | 否 |
| --obfuscateOperationId | 是否混淆操作 ID 与模型名称 | true | 否 |
| --apiGenDir | API Gen 工作目录 | api_gen | 否 |
| --disableModelMapping | 禁用 model 名称映射功能（不加载、不导出、不检查增量），回到旧版本直接生成行为 | false | 否 |
| --modelNameMap | 固定 model 名称映射文件（JSON：原名 → 混淆名）；命中映射时跳过哈希生成。未指定时自动加载默认路径 `{apiGenDir}/model_name_mapping.json`（若存在）作为增量基础 | 无 | 否 |
| --exportModelNameMap | 导出本次 model 名称映射到 JSON 文件；导出时自动合并历史映射 + 本次新映射，支持增量 | {apiGenDir}/model_name_mapping.json | 否 |
| --library | HTTP 客户端库类型。可选值：`jvm-ktor`（Ktor 1.6.7）、`jvm-okhttp4`（OkHttp 4.2.0）、`jvm-spring-webclient`、`jvm-spring-restclient`、`jvm-retrofit2`（默认）、`multiplatform`（Kotlin Multiplatform）、`jvm-volley`、`jvm-vertx` | jvm-retrofit2 | 否 |
| --useRxjava | 是否使用 RxJava3 作为异步方案；为 `false` 时启用 Kotlin Coroutines | true | 否 |

## Gradle 集成

### 方案一：直接在项目中引用

```kotlin
// 1. 自定义依赖配置
val apiGenConfigurable by configurations.creating

dependencies {
    // 2. 添加 API Gen 依赖（+ 表示自动使用最新版本，适合开发调试）
    apiGenConfigurable("com.github.cdAhmad:apigen:+")
    
    // 生产环境建议锁定具体版本号，例如：
    // apiGenConfigurable("com.github.cdAhmad:apigen:1.2.1")
}

// 3. 创建生成任务
tasks.register<JavaExec>("generateSwaggerApi") {
    description = "使用 apiGen 从 Swagger API URL 生成 API 代码"
    group = "generation"
    mainClass.set("com.cdahmod.api_gen.MainKt")
    classpath = apiGenConfigurable
    args = listOf(
        "--outputDir", ".",  // 当前目录
        "--package", "com.example.myapplication.api",
        "--modelPackage", "com.example.myapplication.api.bean",
        "--apiPackage", "com.example.myapplication.api.service",
        "--sourceFolder", "src/main/java",
        "--swaggerApiUrl", "/v2/api-docs",
        "--baseResponseName", "BaseResponse",
        "--salt", "your-random-salt-value-here", // 请替换为随机值
        "--apiName", "Default",
        "--obfuscateOperationId", "false",
        "--apiGenDir", "api_gen", // apiGen 目录（默认在 outputDir 下）
        // "--disableModelMapping", "true", // 禁用 model 映射增量保护（可选）
        // "--modelNameMap", "model_name_mapping.json", // 指定固定映射文件（可选）
        // "--exportModelNameMap", "build/api_gen/model_name_mapping.json", // 指定导出映射路径（可选）
        // "--library", "jvm-retrofit2", // HTTP 客户端库（可选，默认 jvm-retrofit2）
        // "--useRxjava", "false" // 使用协程替代 RxJava3（可选，默认 true）
    )
    workingDir = projectDir
}
```

### Model 名称映射（增量保护）

工具默认会在 `{apiGenDir}/model_name_mapping.json` 中记录原始 model 名称与混淆名的映射关系：

- **第一次运行**：自动生成映射文件，直接完成代码生成。
- **后续运行**：
  - 若 swagger 无新增 model → 直接生成。
  - 若 swagger **新增 model** → 导出新的映射文件后**中断**，提示你确认新增的 model 映射值。检查无误后直接重新运行即可。

该机制确保 model 名称在跨版本迭代中保持稳定。如需关闭此功能，添加 `--disableModelMapping true` 即可回到旧版本行为。

## 生成代码结构

```
<outputDir>/
└── <sourceFolder>/
    └── <package>/
        ├── api/            # API 服务接口
        └── bean/           # 模型类
            └── BaseResponse.kt
```

## 功能特性

- 从 Swagger/OpenAPI 规范生成 Kotlin 代码
- 支持多种 HTTP 客户端库：Retrofit 2、OkHttp 4、Ktor、Spring WebClient/RestClient、Vert.x、Volley 等
- 支持 RxJava3 与 Kotlin Coroutines 两种异步方案
- 使用 kotlinx.serialization 处理 JSON
- 可自定义响应基类
- 支持操作 ID 混淆
- 加密盐值增强安全性
- model 名称混淆映射，支持增量保护（跨版本保持 model 名一致）
- 支持固定 model 名称映射（JSON 文件）

## 常见问题排查

- **缺少 salt**：请确保通过 `--salt` 提供了有效的盐值。建议每个项目使用不同的随机值以增强安全性。
- **swaggerApiUrl 为空**：请提供有效的 Swagger API URL 或文件路径。
- **temp.json 未生成**：请检查 Swagger API URL 是否可访问，并返回有效的 JSON。

## 注意事项

- 生成的代码使用所选 HTTP 客户端库（默认 Retrofit 2）和 kotlinx.serialization
- 请确保项目中已添加必要的依赖
- 响应基类将自动以指定名称创建
- **重要**：请勿将 api-gen 作为 `implementation` 依赖添加。它仅用于代码生成，不应包含在最终发布包中。请按照上述 Gradle 集成示例使用 `apiGenConfigurable` 之类的自定义配置，确保它仅在构建过程中使用。

## 技术支持

如有问题或功能请求，请访问 GitHub 仓库：
https://github.com/cdAhmad/apigen

## 相关工具

- **Swagglog**：Swagger API 日志工具。访问：https://github.com/cdAhmad/swaggerlog
