plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    `maven-publish`
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar() // 👈 推荐：自动附加 sources.jar
    withJavadocJar() // 👈 推荐：自动附加 javadoc.jar
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("org.openapitools:openapi-generator-cli:7.21.0")

}

// --- Maven Publish 配置 ---
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.cdAhmad"
            artifactId = "api-gen"
            version = "1.0.1"

            // ✅ 修正：使用 "java" 而不是 "release"
            from(components["java"])   // ←←← 关键修复！


        }
        
        // --- 发布到 Jitpack 配置 ---
        create<MavenPublication>("shadow") {
            groupId = "com.github.cdAhmad"
            artifactId = "api-gen"
            version = "1.0.1"
            from(components["shadow"])
        }
    }
    repositories {
        // 发布到本地 Maven (~/.m2/repository)
//        mavenLocal()

        // 如果发布到私有远程仓库
        /*
        maven {
            url = uri("https://your.private.repo/repository/maven-releases/")
            credentials {
                username = "admin"
                password = "password"
            }
        }
        */
    }
}

// --- Application 配置 ---
application {
    mainClass = "com.cdahmod.api_gen.MainKt"
}

// --- ShadowJar 配置 ---
shadowJar {
    archiveBaseName.set("api-gen")
    archiveVersion.set("1.0.1")
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.cdahmod.api_gen.MainKt"
    }
}
