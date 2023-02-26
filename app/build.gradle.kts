import org.gradle.internal.os.OperatingSystem
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import java.util.Locale
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    alias(libs.plugins.lsplugin.apktransform)
}

val properties = Properties()
project.rootProject.file("local.properties").let {
    if (it.isFile && it.exists()) {
        properties.load(it.inputStream())
    }
}

fun String.execute(currentWorkingDir: File = file("./")): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        workingDir = currentWorkingDir
        commandLine = split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

val verName = "1.9"
val gitCommitCount = "git rev-list HEAD --count".execute().toInt()
val gitCommitHash = "git rev-parse --verify --short HEAD".execute()

apktransform {
    copy { variant ->
        var suffix = ""
        if (properties.getProperty("buildWithGitSuffix").toBoolean()) {
            suffix += ".r${gitCommitCount}.${gitCommitHash}"
        }
        file("${variant.name}/TwiFucker-V${verName}${suffix}-${variant.name}.apk")
    }
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
    namespace = "icu.nullptr.twifucker"
    compileSdk = 33
    buildToolsVersion = "33.0.2"

    defaultConfig {
        applicationId = "icu.nullptr.twifucker"
        minSdk = 24
        targetSdk = 33
        versionCode = gitCommitCount
        versionName = verName

        if (properties.getProperty("buildWithGitSuffix").toBoolean()) versionNameSuffix =
            ".r${gitCommitCount}.${gitCommitHash}"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
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
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    androidResources.additionalParameters += listOf(
        "--allow-reserved-package-id",
        "--package-id",
        "0x64"
    )

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    dependenciesInfo {
        includeInApk = false
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    compileOnly("de.robv.android.xposed:api:82")
    implementation("org.luckypray:DexKit:1.1.2")
    implementation("com.github.kyuubiran:EzXHelper:2.0.0-RC7")
}

val adbExecutable: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath

val restartTwitter = task("restartTwitter").doLast {
    Runtime.getRuntime().let {
        it.exec("$adbExecutable shell am force-stop com.twitter.android").waitFor()
        it.exec("$adbExecutable shell am start $(pm resolve-activity --components com.twitter.android)")
            .waitFor()
    }
}

afterEvaluate {
    tasks.named("installDebug") {
        finalizedBy(restartTwitter)
    }
}
