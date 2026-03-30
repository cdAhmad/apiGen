plugins {
    alias(libs.plugins.android.application)
    alias (libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.myapplication"
    compileSdk =34

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

dependencies {
    implementation(libs.appcompat.v7)
    testImplementation(libs.junit)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.espresso.core)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
}

// 添加生成 API 的任务
tasks.register<Exec>("generateApi") {
    description = "Generate API code using apiGen"
    
    // 配置执行命令，使用本地的 apiGen jar 文件，并传递命令行参数
    commandLine = listOf(
        "java",
        "-jar",
        "..\\api_gen\\build\\libs\\api-gen-1.0.1.jar",
        "--outputDir", "",
        "--package", "com.example.myapplication.api",
        "--modelPackage", "com.example.myapplication.api.bean",
        "--apiPackage", "com.example.myapplication.api.service",
        "--sourceFolder", "src/main/java",
//        "--swaggerApiUrl", "/api-docs",
        "--baseResponseName", "BaseResponse",
        "--salt", "myapp-salt-123",
        "--apiName", "Default",
        "--obfuscateOperationId", "false"
    )
    
    // 设置工作目录
    workingDir = projectDir
}