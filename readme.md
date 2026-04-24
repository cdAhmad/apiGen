#  swagger 接口文档转 koltin 代码

[![Release](https://www.jitpack.io/v/cdAhmad/apigen.svg)]

### 引入方式
1. 直接项目中引用
2. 作为单独module 引入

## 1. 直接项目中引用

```kotlin
// 1. 自定义依赖 
val apiGenConfigurable by configurations.creating

dependencies {
    // 2. 依赖 apiGenConfigurable 配置
    apiGenConfigurable("com.github.cdAhmad:apigen:+") // + 自动使用最新版本，生产环境建议锁定具体版本号
}

// 3. 创建任务 generateSwaggerApi
tasks.register<JavaExec>("generateSwaggerApi") {
    description = "Generate API code using apiGen from swagger api url"
    group = "generation"
    mainClass.set("com.cdahmod.api_gen.MainKt")
    classpath =apiGenConfigurable
    args = listOf(
        "--outputDir", "",  // 指定输出目录 空字符串当前路径
        "--package", "com.example.myapplication.api", // 指定生成的代码的包名
        "--modelPackage", "com.example.myapplication.api.bean",// 模型包名
        "--apiPackage", "com.example.myapplication.api.service",// API 包名
        "--sourceFolder", "src/main/java",// 生成文件目录
        "--swaggerApiUrl", "/v2/api-docs", //swagger api url
        "--baseResponseName", "BaseResponse", // 响应基类名称
        "--salt", "myapp-salt-123", // 加密盐值
        "--apiName", "Default", // API 名称
        "--obfuscateOperationId", "false", // 是否混淆操作请求名称
        "--apiGenDir", "build/api_gen" // apiGen 目录
    )
    // 设置工作目录
    workingDir = projectDir
}

``` 

## 2. 作为单独module 引入


``` kotlin
// 1. 自定义依赖 
val apiGenConfigurable by configurations.creating

dependencies {
    // 2. 依赖 apiGenConfigurable 配置
    apiGenConfigurable("com.github.cdAhmad:apigen:+") // + 自动使用最新版本，生产环境建议锁定具体版本号
}

// 3. 创建任务 generateSwaggerApi
tasks.register<JavaExec>("generateSwaggerApi") {
    description = "Generate API code using apiGen from swagger api url"
    group = "generation"
    mainClass.set("com.cdahmod.api_gen.MainKt")
    classpath =apiGenConfigurable
    args = listOf(
        "--outputDir", "",  // 指定输出目录 空字符串当前路径
        "--package", "com.example.myapplication.api", // 指定生成的代码的包名
        "--modelPackage", "com.example.myapplication.api.bean",// 模型包名
        "--apiPackage", "com.example.myapplication.api.service",// API 包名
        "--sourceFolder", "src/main/java",// 生成文件目录
        "--swaggerApiUrl", "/v2/api-docs", //swagger api url
        "--baseResponseName", "BaseResponse", // 响应基类名称
        "--salt", "myapp-salt-123", // 加密盐值
        "--apiName", "Default", // API 名称
        "--obfuscateOperationId", "false", // 是否混淆操作请求名称
        "--apiGenDir", "build/api_gen" // apiGen 目录
    )
    // 设置工作目录
    workingDir = projectDir
}

```


