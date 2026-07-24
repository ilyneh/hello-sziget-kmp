import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.playPublisher)
}

val keystorePropertiesFile = file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

// --- Git-based versioning scheme -------------------------------------------------------------
//
// versionCode: total commit count on HEAD (`git rev-list --count HEAD`). Monotonically increasing
// as long as history isn't rewritten (rebases/force-pushes that drop commits could in theory lower
// it, but that's true of any scheme and isn't specific to this one). Works identically for local/
// manual builds and CI builds without needing any external build-number service or persisted state.
//
// versionName: "<base>-<commitCount>-<shortSha>", e.g. "1.0-292-c9327b4". Combined with the
// existing per-build-type versionNameSuffix, a tester sees e.g. "1.0-292-c9327b4-beta" or
// "1.0-292-c9327b4-debug" — human-readable, sortable by commit count, and pinpoints the exact
// commit a bug report came from.
//
// Guarding against a shallow clone: a shallow clone (e.g. CI doing `git fetch --depth=1`) makes
// `git rev-list --count HEAD` return the depth (often just 1) instead of the true commit count,
// which would not be monotonic across builds at different commits — every shallow build would
// report versionCode 1. Rather than silently emitting a wrong-but-plausible-looking number, we
// detect this case (`git rev-parse --is-shallow-repository`) and fall back to the static baseline,
// printing a loud warning so CI can be fixed to do a full clone (`fetch-depth: 0`) instead of
// masking the problem with a "correct-looking" but meaningless value.
//
// No-git fallback: if git isn't available at all (e.g. building from a source archive with no
// .git directory), we fall back to the same static baseline rather than failing configuration —
// a working build with a non-unique version is a smaller regression than a hard failure.
val versioningBaseName = "1.0"
val versioningFallbackCode = 1
val versioningFallbackName = "$versioningBaseName-nogit"

fun execGitOutput(vararg args: String): String? =
    runCatching {
        providers
            .exec {
                commandLine("git", *args)
                isIgnoreExitValue = false
            }.standardOutput.asText
            .get()
            .trim()
    }.getOrNull()

val gitIsShallow = execGitOutput("rev-parse", "--is-shallow-repository") == "true"

val computedVersionCode: Int =
    if (!gitIsShallow) {
        execGitOutput("rev-list", "--count", "HEAD")?.toIntOrNull() ?: versioningFallbackCode
    } else {
        logger.warn(
            "androidApp: git repository is a shallow clone — falling back to a static " +
                "versionCode ($versioningFallbackCode) instead of the (meaningless, non-monotonic) " +
                "shallow commit count. CI should fetch full history (e.g. `fetch-depth: 0` on " +
                "actions/checkout) so versionCode can be computed correctly.",
        )
        versioningFallbackCode
    }

val computedVersionName: String =
    if (!gitIsShallow) {
        val shortSha = execGitOutput("rev-parse", "--short", "HEAD")
        if (shortSha != null) {
            "$versioningBaseName-$computedVersionCode-$shortSha"
        } else {
            versioningFallbackName
        }
    } else {
        versioningFallbackName
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
        versionCode = computedVersionCode
        versionName = computedVersionName
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

// --- Google Play publishing (Gradle Play Publisher) -------------------------------------------
//
// Publishes the "beta" build type's AAB to the Play Console "beta" track. Because the "beta"
// build type carries applicationIdSuffix ".beta", it ships as its own package
// (com.ilyne.helloszigetkmp.beta) with its own Play Console app listing, separate from the
// release app (com.ilyne.helloszigetkmp) — that listing must already exist in Play Console
// before publishBetaBundle can succeed, since Play Publisher only publishes to an app that's
// already been created there (it can't create a brand-new app listing).
//
// serviceAccountCredentials points at a service account JSON key file. Locally this file doesn't
// exist and any local `./gradlew publishBetaBundle` invocation will fail fast with a clear
// "file not found" error, same pattern as the release keystore below — the GitHub Actions
// workflow (.github/workflows/play-beta-release.yml) writes the key from a repo secret to this
// path before invoking the publish task.
play {
    serviceAccountCredentials.set(file("play-service-account.json"))
    track.set("internal")
    defaultToAppBundles.set(true)
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
}
