---
name: "api-gen"
description: "Converts Swagger API documentation to Kotlin code. Invoke when user needs to generate Kotlin API code from Swagger/OpenAPI specifications."
---

# API Gen Skill

## Overview

API Gen is a tool that converts Swagger API documentation to Kotlin code, specifically designed for Android/Java projects using Retrofit 2. It generates:
- API service interfaces
- Model classes
- Base response class

## Getting Started

### Prerequisites
- Java 11 or higher
- A Swagger/OpenAPI JSON endpoint or file

### Automatic Configuration

When you invoke this skill, it will:

1. **Analyze your project structure** to determine the appropriate configuration
2. **Ask for the Swagger API URL or file path** (required)
3. **Predict optimal settings** based on your project's characteristics
4. **Present the proposed configuration** for your review
5. **Ask for your confirmation** to ensure the configuration is correct
6. **Execute the generation task** only after your confirmation

This progressive approach ensures that you get the most suitable configuration for your project while maintaining full control over the process.

### Important Note

The Swagger API URL or file path is **required** for this tool to work. You will be prompted to provide this information when invoking the skill.

### Usage

#### Using via JitPack

#### Gradle
```groovy
repositories {
    maven {
        url 'https://www.jitpack.io'
    }
}


```

#### Using the JAR File (Alternative)

If you prefer to use the JAR file directly, download it from:
https://www.jitpack.io/#cdAhmad/apigen

```bash
java -jar api-gen-1.0.3.jar [options]
```

#### Command Line Options

| Option | Description | Default | Required |
|--------|-------------|---------|----------|
| --outputDir | Output directory for generated code | generated-code | No |
| --package | Base package name | com.temp.net | No |
| --modelPackage | Model classes package name | {package}.bean | No |
| --apiPackage | API service interfaces package name | {package}.api | No |
| --sourceFolder | Source folder name | src/main/kotlin | No |
| --swaggerApiUrl | Swagger API URL or file path | | Yes |
| --baseResponseName | Base response class name | BaseResponse | No |
| --salt | Encryption salt (required for security) | | Yes |
| --apiName | API name | Default | No |
| --obfuscateOperationId | Whether to obfuscate operation IDs | true | No |
| --apiGenDir | API Gen working directory | .api_gen | No |

### Example

```bash
java -jar api-gen-1.0.3.jar \
    --outputDir "." \
    --package "com.example.myapplication.api" \
    --modelPackage "com.example.myapplication.api.bean" \
    --apiPackage "com.example.myapplication.api.service" \
    --sourceFolder "src/main/java" \
    --swaggerApiUrl "http://localhost:8080/v2/api-docs" \
    --baseResponseName "BaseResponse" \
    --salt "your-random-salt-value-here" \
    --apiName "Default" \
    --obfuscateOperationId "false" \
    --apiGenDir "build/api_gen"
```

**Note**: Replace `your-random-salt-value-here` with a random value for each project.

## Integration with Gradle

### Option 1: Direct Project Reference

```kotlin
// 1. Custom dependency configuration
val apiGenConfigurable by configurations.creating

dependencies {
    // 2. Add API Gen dependency
    apiGenConfigurable("com.github.cdAhmad:apigen:1.0.3")
}

// 3. Create generation task
tasks.register<JavaExec>("generateSwaggerApi") {
    description = "Generate API code using apiGen from swagger api url"
    group = "generation"
    mainClass.set("com.cdahmod.api_gen.MainKt")
    classpath = apiGenConfigurable
    args = listOf(
        "--outputDir", ".",  // Current directory
        "--package", "com.example.myapplication.api",
        "--modelPackage", "com.example.myapplication.api.bean",
        "--apiPackage", "com.example.myapplication.api.service",
        "--sourceFolder", "src/main/java",
        "--swaggerApiUrl", "/v2/api-docs",
        "--baseResponseName", "BaseResponse",
        "--salt", "your-random-salt-value-here", // Replace with a random value
        "--apiName", "Default",
        "--obfuscateOperationId", "false",
        "--apiGenDir", "build/api_gen"
    )
    workingDir = projectDir
}
```

### Option 2: As Separate Module

Same configuration as Option 1, but in a dedicated module.

## Generated Code Structure

```
<outputDir>/
└── <sourceFolder>/
    └── <package>/
        ├── api/            # API service interfaces
        └── bean/           # Model classes
            └── BaseResponse.kt
```

## Features

- Generates Kotlin code from Swagger/OpenAPI specifications
- Supports Retrofit 2 with coroutines
- Uses kotlinx.serialization for JSON handling
- Customizable base response class
- Obfuscation option for operation IDs
- Encrypted salt for security

## Troubleshooting

- **salt is required**: Ensure you provide a valid salt value using `--salt`. Use a random value for each project to enhance security.
- **swaggerApiUrl is blank**: Ensure you provide a valid Swagger API URL or file path
- **temp.json not generated**: Check if the Swagger API URL is accessible and returns valid JSON

## Notes

- The generated code uses Retrofit 2 with coroutines and kotlinx.serialization
- Make sure to add the necessary dependencies to your project
- The base response class will be created automatically with the specified name
- **Important**: Do not add api-gen as an implementation dependency. It should only be used for code generation and not included in the final release. Use a custom configuration like `apiGenConfigurable` as shown in the Gradle integration examples to ensure it's only used during the build process.

## Support

For issues or feature requests, please visit the GitHub repository:
https://github.com/cdAhmad/apigen

## Related Tools

- **Swagglog**: A logging tool for Swagger APIs. Visit: https://github.com/cdAhmad/swaggerlog
