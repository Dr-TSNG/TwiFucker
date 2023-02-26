buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0-alpha06")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
