import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.spotless)
}

// --- Build-time app configuration -------------------------------------------------------------
//
// Lets a build point at a local/localhost backend, skip Google Sign-In, and swap the dev bearer
// token, without editing source. Resolved the same layered way androidApp resolves
// keystore.properties: a Gradle project property (`-Psziget.xxx=...`) wins if set — this also
// covers CI (`-P` flags) and Xcode's embedAndSignAppleFrameworkForXcode Gradle invocation, since
// both go through the same Gradle project properties mechanism — otherwise falls back to
// `local.properties` at the repo root (gitignored, so it's safe to put a developer's own
// machine-specific values there, e.g. a LAN IP or a personal test token). See
// local.properties.example for the available keys.
val rootLocalPropertiesFile = rootProject.file("local.properties")
val rootLocalProperties = Properties().apply {
    if (rootLocalPropertiesFile.exists()) {
        rootLocalPropertiesFile.inputStream().use { load(it) }
    }
}

fun resolveConfigProperty(key: String): String? =
    (project.findProperty(key) as String?) ?: rootLocalProperties.getProperty(key)

// GenerateSzigetBuildConfigTask lives in buildSrc/src/main/kotlin — see its kdoc for why it's a
// dedicated task type rather than an ad-hoc `tasks.registering { doLast { ... } }`.
val generateSzigetBuildConfig by tasks.registering(GenerateSzigetBuildConfigTask::class) {
    localBackendUrl.set(resolveConfigProperty("sziget.localBackendUrl").orEmpty())
    localBearerToken.set(resolveConfigProperty("sziget.localBearerToken").orEmpty())
    skipGoogleSignIn.set(resolveConfigProperty("sziget.skipGoogleSignIn")?.toBoolean() ?: false)
    outputDir.set(layout.buildDirectory.dir("generated/szigetConfig/commonMain/kotlin"))
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
        namespace = "com.ilyne.helloszigetkmp.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
            // Unmocked android.util.Log throws by default under this host-test runner (there's
            // no Robolectric shadow installed) — any code path that calls Logger.e (which is
            // most of the app's catch blocks) would otherwise crash the test instead of letting
            // it assert on the resulting UI state.
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        commonMain.configure {
            kotlin.srcDir(generateSzigetBuildConfig.flatMap { it.outputDir })
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.coil.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.credentials)
            implementation(libs.credentials.play.services)
            implementation(libs.googleid)
            implementation(libs.security.crypto)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Navigation
            implementation(libs.navigation.compose)
            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            // Ktor
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            // Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            // Room
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            // Settings
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.turbine)
        }
    }
}

// KSP for Room code generation on each target
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    androidRuntimeClasspath(libs.compose.uiTooling)
}

room {
    schemaDirectory("$projectDir/schemas")
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt") // Exclude generated KMP files

        // ktlint owns both formatting and the io.nlopez.compose.rules custom "compose"
        // ruleset (compositionlocal-allowlist, lambda-param-in-effect, function-naming
        // exception for @Composable). no-wildcard-imports is disabled because Compose
        // files intentionally wildcard-import androidx.compose.* packages.
        //
        // multiline-expression-wrapping is disabled so the callee stays on the same line
        // as `=` (e.g. `val x = listOf(` with only the argument list wrapping below it).
        // This previously had a comment here claiming Spotless silently ignored this
        // override even though the raw ktlint CLI respected it. Re-tested 2026-07 against
        // Spotless 8.8.0 / ktlint 1.8.0 with several representative cases (val assignments
        // to constructor/function calls, when/if-else expressions, elvis chains, boolean
        // chains) and could not reproduce that — the override works correctly here now.
        // Leaving the property set is still correct either way; if re-wrapping shows up
        // again, verify in an isolated `git worktree` before re-adding a workaround.
        ktlint("1.8.0")
            .customRuleSets(
                listOf(
                    "io.nlopez.compose.rules:ktlint:0.6.2",
                ),
            ).editorConfigOverride(
                mapOf(
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    "compose_allowed_composition_locals" to "LocalAppColors",
                    "ktlint_standard_multiline-expression-wrapping" to "disabled",
                    "ktlint_standard_string-template-indent" to "disabled",
                ),
            )
    }
}
