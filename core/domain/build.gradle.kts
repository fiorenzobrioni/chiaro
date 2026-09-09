// Pure Kotlin/JVM, and it must stay that way: the engines Chiaro inherited from
// tweather (the ephemeris, the verdicts, the rules, the alert buckets) have no
// Android in them, and the module that holds them is where that is enforced.
// If a class here ever needs a Context or a Resources, it is in the wrong module.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// 17 everywhere, set the way the Android modules set it rather than through a
// toolchain: a toolchain would demand a JDK 17 on every machine that builds this,
// and the JDK that ships with Android Studio is not one.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// The warning-zone asset ships from :core:data (it is an Android asset there) and is
// read by the domain's WarningZoneIndex as a string. Its test lives here, with the
// index, and reads the very file the app ships rather than a copy that could drift:
// hence the data module's assets directory as a TEST resource of this module.
sourceSets {
    test {
        resources.srcDir("../data/src/main/assets")
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.cron.utils)
}
