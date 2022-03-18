import java.io.ByteArrayOutputStream
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
}

val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

val verName = "1.2"
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

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "icu.nullptr.twifucker"
        minSdk = 24
        targetSdk = 32
        versionCode = gitCommitCount
        versionName = verName

        if (properties.getProperty("buildWithGitSuffix").toBoolean())
            versionNameSuffix = ".r${gitCommitCount}.${gitCommitHash}"
    }

    signingConfigs.create("config") {
        storeFile = file(properties.getProperty("fileDir"))
        storePassword = properties.getProperty("storePassword")
        keyAlias = properties.getProperty("keyAlias")
        keyPassword = properties.getProperty("keyPassword")
    }

    buildTypes {
        signingConfigs.named("config").get().also {
            release {
                signingConfig = it
                isMinifyEnabled = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
        }
    }

    androidResources.additionalParameters("--allow-reserved-package-id", "--package-id", "0x64")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

fun afterEval() = android.applicationVariants.forEach { variant ->
    val variantCapped = variant.name.capitalize()
    val packageTask = tasks["package$variantCapped"]
    task<Copy>("build$variantCapped") {
        dependsOn(packageTask)
        into("$buildDir/outputs/apk/${variant.name}")
        from(packageTask.outputs) {
            include("*.apk")
            rename(".*\\.apk", "TwiFucker-V${variant.versionName}-${variant.name}.apk")
        }
    }
}

afterEvaluate {
    afterEval()
}

dependencies {
    implementation("com.github.kyuubiran:EzXHelper:0.7.5")

    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("de.robv.android.xposed:api:82:sources")
}
