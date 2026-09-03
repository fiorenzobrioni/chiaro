// Android library: the single periodic background job shared by sync, alerts, rules
// and sky observation (Fase 6, VISION §7.1). The notifiers it calls are TEXT and
// text is presentation, so they live in :app behind [SyncNotifiers] — this module
// decides WHEN to speak, never what to say.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.callbackdev.chiaro.core.sync"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        getByName("main") { java.srcDirs("src/main/kotlin") }
        getByName("test") { java.srcDirs("src/test/kotlin") }
    }
}

dependencies {
    api(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
}
