import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.credentials)
            implementation(libs.credentials.play.services)
            implementation(libs.googleid)
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
        // We'd also like multiline-expression-wrapping disabled (keep the callee on the
        // same line as `=`, e.g. `val x = listOf(` with only the argument list wrapping
        // below it) — confirmed via the raw ktlint CLI that disabling this rule via
        // .editorconfig works correctly and produces exactly that output. But Spotless's
        // ktlint integration silently drops this specific property no matter how it's
        // supplied (editorConfigOverride map, or an explicit editorConfigPath pointing at
        // this exact file) — every other override here takes effect except this one, so
        // it appears to be a gap in Spotless's property bridge, not a ktlint limitation.
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
