plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    `maven-publish`
    application
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
val JarVersion="1.2.1"

// --- Maven Publish 配置 ---
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.cdAhmad"
            artifactId = "api-gen"
            version = JarVersion

            // 使用包含所有依赖的 jar 文件
            artifact(tasks.jar)
        }
        

    }
    repositories {

    }
}




// --- Application 配置 ---
application {
    mainClass = "com.cdahmod.api_gen.MainKt"
}

// --- Jar 配置 ---
tasks.jar {
    archiveBaseName.set("api-gen")
    archiveVersion.set(JarVersion)
    manifest {
        attributes["Main-Class"] = "com.cdahmod.api_gen.MainKt"
    }
    // 包含所有依赖
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    // 处理重复条目
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
