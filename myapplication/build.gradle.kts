plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

val apiGenConfigurable by configurations.creating
dependencies {
    implementation(libs.appcompat.v7)
    testImplementation(libs.junit)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.espresso.core)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    apiGenConfigurable("com.github.cdAhmad:apigen:1.0.2")
}


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

 // 测试使用 apiGen 生成 API 代码
tasks.register<Exec>("generateApi") {
    description = "Generate API code using apiGen"

    // 配置执行命令，使用本地的 apiGen jar 文件，并传递命令行参数
    commandLine = listOf(
        "java",
        "-jar",
        "..\\api_gen\\build\\libs\\api-gen-1.0.3.jar",
        "--outputDir", "",
        "--package", "com.example.myapplication.api",
        "--modelPackage", "com.example.myapplication.api.bean",
        "--apiPackage", "com.example.myapplication.api.service",
        "--sourceFolder", "src/main/java",
        "--swaggerApiUrl", "/v2/api-docs",
        "--baseResponseName", "BaseResponse",
        "--salt", "myapp-salt-123",
        "--apiName", "Default",
        "--obfuscateOperationId", "false",
        "--apiGenDir", "build/api_gen"
    )
    // 设置工作目录
    workingDir = projectDir
    // 确保 api_gen 项目已构建
    dependsOn(":api_gen:jar")
}