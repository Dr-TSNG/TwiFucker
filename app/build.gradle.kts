import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
}

val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

val verName = "1.2.1"
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
            "$adb shell am start com.twitter.android/com.twitter.android.StartActivity".execute()
        }
    }
}

dependencies {
    implementation("com.github.kyuubiran:EzXHelper:0.7.5")

    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("de.robv.android.xposed:api:82:sources")
}
