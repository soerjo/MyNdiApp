plugins {
    id("com.android.library")
}

import org.apache.tools.ant.taskdefs.condition.Os
import java.util.Properties

android {
    namespace = "com.jiangdg.uvccamera"
    compileSdk = 35
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 24
        // targetSdk is not valid for library modules
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
        disable += "MissingTranslation"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

// Only configure ndkBuild if NDK is available
// Get NDK directory from SDK or environment
val ndkDirProvider = provider {
    System.getenv("NDK_HOME") ?: run {
        try {
            // Try to read from local.properties (for backward compatibility)
            val properties = Properties()
            val localPropsFile = project.rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                properties.load(localPropsFile.inputStream())
                properties.getProperty("ndk.dir")
            } else null
        } catch (e: Exception) {
            null
        }
    } ?: run {
        // Fallback to default SDK location
        val sdkDir = project.rootProject.file("local.properties").let { file ->
            try {
                val props = Properties()
                if (file.exists()) {
                    props.load(file.inputStream())
                    props.getProperty("sdk.dir")?.let { sdkPath ->
                        // Convert Windows path separators if needed
                        sdkPath.replace("\\", "/")
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }
        // Use Android SDK's NDK
        sdkDir?.let { "$it/ndk/27.0.12077973" }
    }
}

val ndkDir = ndkDirProvider.get()

if (ndkDir != null) {
    val ndkBuildExecutable = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        "$ndkDir/ndk-build.cmd"
    } else {
        "$ndkDir/ndk-build"
    }

    // Check if ndk-build exists before registering tasks
    val ndkBuildFile = file(ndkBuildExecutable)
    if (ndkBuildFile.exists()) {
        tasks.register("ndkBuild", Exec::class) {
            group = "build"
            description = "Compile JNI source via NDK"

            doFirst {
                println("executing ndkBuild: $ndkBuildExecutable")
            }

            commandLine(ndkBuildExecutable, "-j8", "-C", file("src/main").absolutePath)
        }

        tasks.register("ndkClean", Exec::class) {
            group = "build"
            description = "Clean JNI libraries"

            doFirst {
                println("executing ndkBuild clean")
            }

            commandLine(ndkBuildExecutable, "clean", "-C", file("src/main").absolutePath)
        }

        tasks.named("clean") {
            dependsOn("ndkClean")
        }

        tasks.withType<JavaCompile>().configureEach {
            dependsOn("ndkBuild")
        }
    } else {
        logger.warn("NDK build executable not found at: $ndkBuildExecutable")
    }
} else {
    logger.warn("NDK not configured. Native libraries will not be built. Please set android.ndkVersion in build.gradle.kts.")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.elvishew:xlog:1.11.0")
}
