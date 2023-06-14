import org.gradle.internal.os.OperatingSystem
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.lsparanoid)
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

val verName = "2.0"
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

lsparanoid {
    global = true
    includeDependencies = true
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
    ndkVersion = "25.2.9519653"
    buildToolsVersion = "33.0.2"

    defaultConfig {
        applicationId = "icu.nullptr.twifucker"
        minSdk = 27
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

    buildFeatures {
        prefab = true
    }

    externalNativeBuild.ndkBuild {
        path("src/main/cpp/Android.mk")
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
    compileOnly(libs.legacy.xposed.api)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.recyclerview)
    implementation(libs.dexkit)
    implementation(libs.ezxhelper)
    implementation(libs.mmkv)
    implementation(libs.ndk.cxx)
    implementation(libs.ndk.nativehelper)
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
    tasks.named("installRelease") {
        finalizedBy(restartTwitter)
    }
}
