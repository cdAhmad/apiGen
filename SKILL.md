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
| --apiGenDir | API Gen 工作目录 | .api_gen | 否 |

## Gradle 集成

### 方案一：直接在项目中引用

```kotlin
// 1. 自定义依赖配置
val apiGenConfigurable by configurations.creating

dependencies {
    // 2. 添加 API Gen 依赖（+ 表示自动使用最新版本，适合开发调试）
    apiGenConfigurable("com.github.cdAhmad:apigen:+")
    
    // 生产环境建议锁定具体版本号，例如：
    // apiGenConfigurable("com.github.cdAhmad:apigen:1.1.0")
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
        "--apiGenDir", "build/api_gen"
    )
    workingDir = projectDir
}
```

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
- 支持 Retrofit 2 与协程
- 使用 kotlinx.serialization 处理 JSON
- 可自定义响应基类
- 支持操作 ID 混淆
- 加密盐值增强安全性

## 常见问题排查

- **缺少 salt**：请确保通过 `--salt` 提供了有效的盐值。建议每个项目使用不同的随机值以增强安全性。
- **swaggerApiUrl 为空**：请提供有效的 Swagger API URL 或文件路径。
- **temp.json 未生成**：请检查 Swagger API URL 是否可访问，并返回有效的 JSON。

## 注意事项

- 生成的代码使用 Retrofit 2 协程和 kotlinx.serialization
- 请确保项目中已添加必要的依赖
- 响应基类将自动以指定名称创建
- **重要**：请勿将 api-gen 作为 `implementation` 依赖添加。它仅用于代码生成，不应包含在最终发布包中。请按照上述 Gradle 集成示例使用 `apiGenConfigurable` 之类的自定义配置，确保它仅在构建过程中使用。

## 技术支持

如有问题或功能请求，请访问 GitHub 仓库：
https://github.com/cdAhmad/apigen

## 相关工具

- **Swagglog**：Swagger API 日志工具。访问：https://github.com/cdAhmad/swaggerlog
