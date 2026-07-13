import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

val keystorePropertiesFile = file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

// Fail loudly (instead of silently producing an unsigned APK/AAB) when a release/beta build is
// actually requested without a keystore.properties in place. Debug builds are unaffected — they
// use AGP's auto-generated debug keystore and don't reference signingConfigs["release"] below.
gradle.taskGraph.whenReady {
    val requestedReleaseLikeBuild = gradle.startParameter.taskNames.any { name ->
        name.contains("Release", ignoreCase = true) || name.contains("Beta", ignoreCase = true)
    }
    if (requestedReleaseLikeBuild && !keystorePropertiesFile.exists()) {
        throw GradleException(
            "androidApp/keystore.properties is missing — release/beta builds must be signed. " +
                "Copy androidApp/keystore.properties.example to androidApp/keystore.properties " +
                "and fill in your release keystore's details.",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
}

android {
    namespace = "com.ilyne.helloszigetkmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ilyne.helloszigetkmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
            // Enable automatic symbol and mapping file uploads
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }
        }
        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
