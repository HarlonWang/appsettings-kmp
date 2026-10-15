import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    android {
        namespace = "wang.harlon.appsettings"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // 公开签名里直接出现 Composable lambda、Modifier、ImageVector、SheetState、ColumnScope，
            // 宿主必须在编译期看得见这四个包的类型，故用 api
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            // Settings 出现在 rememberXxxState 的签名里
            api(libs.multiplatform.settings)
            // 宿主不传 Settings 时库自建一个
            implementation(libs.multiplatform.settings.noarg)
            // ThemeStyle.paletteStyle 暴露 PaletteStyle，ColorSource.Fixed 接 ColorScheme
            api(libs.material.kolor)
            implementation(libs.androidx.graphics.shapes)
            implementation(libs.compose.components.resources)
            // 图标直接用 extended（含 core），不再内联；两个宿主本来就依赖它
            implementation(libs.compose.icons.extended)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            // enableEdgeToEdge / SystemBarStyle：按深浅设置系统栏
            implementation(libs.androidx.activity)
            // AppCompatDelegate.setApplicationLocales：界面语言的 per-app locale
            implementation(libs.androidx.appcompat)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.junit)
        }
    }
}

compose.resources {
    // 固定资源类的包名，不跟工程名走
    packageOfResClass = "wang.harlon.appsettings.resources"
}

mavenPublishing {
    publishToMavenCentral()
    // CI 注入 signingInMemoryKey 时启用签名；本地无密钥跳过
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    coordinates(groupId = "wang.harlon", artifactId = "appsettings-kmp")

    pom {
        name.set("appsettings-kmp")
        description.set("Compose Multiplatform app settings toolkit: appearance, about, what's new and a shared settings UI kit.")
        url.set("https://github.com/HarlonWang/appsettings-kmp")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("HarlonWang")
                name.set("HarlonWang")
                url.set("https://github.com/HarlonWang")
            }
        }
        scm {
            url.set("https://github.com/HarlonWang/appsettings-kmp")
            connection.set("scm:git:git://github.com/HarlonWang/appsettings-kmp.git")
            developerConnection.set("scm:git:ssh://git@github.com/HarlonWang/appsettings-kmp.git")
        }
    }
}
