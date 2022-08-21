import java.io.ByteArrayOutputStream
import java.util.Properties
import java.nio.file.Paths
import org.gradle.internal.os.OperatingSystem

plugins {
    id("com.android.application")
    kotlin("android")
}

val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

val verName = "1.4"
val gitCommitCount = "git rev-list HEAD --count".execute().toInt()
val gitCommitHash = "git rev-parse --verify --short HEAD".execute()

fun String.execute(currentWorkingDir: File = file("./")): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        workingDir = currentWorkingDir
        commandLine = split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

fun findInPath(executable: String): String? {
    val pathEnv = System.getenv("PATH")
    return pathEnv.split(File.pathSeparator).map { folder ->
        Paths.get("${folder}${File.separator}${executable}${if (OperatingSystem.current().isWindows) ".exe" else ""}")
            .toFile()
    }.firstOrNull { path ->
        path.exists()
    }?.absolutePath
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "icu.nullptr.twifucker"
        minSdk = 24
        targetSdk = 33
        versionCode = gitCommitCount
        versionName = verName

        if (properties.getProperty("buildWithGitSuffix").toBoolean()) versionNameSuffix =
            ".r${gitCommitCount}.${gitCommitHash}"

        externalNativeBuild {
            cmake {
                abiFilters("arm64-v8a")
                findInPath("ccache")?.let {
                    println("Using ccache $it")
                    arguments += listOf(
                        "-DANDROID_CCACHE=$it",
                        "-DCMAKE_C_COMPILER_LAUNCHER=ccache",
                        "-DCMAKE_CXX_COMPILER_LAUNCHER=ccache",
                        "-DNDK_CCACHE=ccache"
                    )
                }
            }
        }
    }

    val config = properties.getProperty("fileDir")?.let {
        signingConfigs.create("config") {
            storeFile = file(it)
            storePassword = properties.getProperty("storePassword")
            keyAlias = properties.getProperty("keyAlias")
            keyPassword = properties.getProperty("keyPassword")
        }
    }

    buildTypes {
        all {
            signingConfig = config ?: signingConfigs["debug"]
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        prefab = true
    }

    androidResources {
        noCompress("libtwifucker.so")
    }

    androidResources.additionalParameters("--allow-reserved-package-id", "--package-id", "0x64")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    dependenciesInfo {
        includeInApk = false
    }

    externalNativeBuild {
        cmake {
            path("src/main/jni/CMakeLists.txt")
            version = "3.22.1+"
        }
    }
    buildToolsVersion = "33.0.0"
}

afterEvaluate {
    android.applicationVariants.forEach { variant ->
        val variantCapped = variant.name.capitalize()
        val packageTask = tasks["package$variantCapped"]

        task<Sync>("build$variantCapped") {
            dependsOn(packageTask)
            into("$buildDir/outputs/apk/${variant.name}")
            from(packageTask.outputs) {
                include("*.apk")
                rename(".*\\.apk", "TwiFucker-V${variant.versionName}-${variant.name}.apk")
            }
        }
    }

    val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath
    task("installAndStartTwitter") {
        dependsOn("buildDebug")
        doLast {
            val apk = file("$buildDir/outputs/apk/debug").listFiles()!!.single().absolutePath
            "$adb install $apk".execute()
            "$adb shell am force-stop com.twitter.android".execute()
            "$adb shell am start com.twitter.android/com.twitter.android.StartActivity".execute()
        }
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.4.0")
    implementation("com.github.kyuubiran:EzXHelper:1.0.1")

    compileOnly("de.robv.android.xposed:api:82")
}
