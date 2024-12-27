import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.littletrickster.scanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.littletrickster.scanner"
        minSdk = 21
        targetSdk = 34
        versionCode = 4
        versionName = "1.03"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // example usage
        // gradlew -PsingleAbi="arm" assembleRelease

        when (project.properties["singleAbi"]) {


            "arm" -> {
                setProperty("archivesBaseName", "app-arm")
                ndk {
                    abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
                }
            }


            "x86" -> {
                setProperty("archivesBaseName", "app-x86")
                ndk {
                    abiFilters.addAll(listOf("x86"))
                }
            }

            "x86_64" -> {
                setProperty("archivesBaseName", "app-x86_64")
                ndk {
                    abiFilters.addAll(listOf("x86_64"))
                }
            }

            "arm64-v8a" -> {
                setProperty("archivesBaseName", "app-arm64-v8a")
                ndk {
                    abiFilters.addAll(listOf("arm64-v8a"))
                }
            }

            "armeabi-v7a" -> {
                setProperty("archivesBaseName", "app-armeabi-v7a")
                ndk {
                    abiFilters.addAll(listOf("armeabi-v7a"))
                }
            }

            "universal" -> {
                setProperty("archivesBaseName", "app-universal")
                ndk {
                    abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86"))
                }
            }
        }

    }

    splits {
        abi {

            isEnable = project.properties["singleAbi"] == null

            reset()

            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")

            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(
            "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=com.google.accompanist.pager.ExperimentalPagerApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    aaptOptions {
        noCompress("tflite") // Your model's file extension: "tflite", "lite", etc.
    }
}

val compose_version = "1.6.5"

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.material:material:$compose_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation("androidx.compose.material:material-icons-extended:1.6.5")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation(project(path = ":opencv"))
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
//    implementation "com.quickbirdstudios:opencv-contrib:4.5.3.0"
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    implementation("androidx.preference:preference-ktx:1.2.1")

    implementation("com.tom-roush:pdfbox-android:2.0.27.0")


    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")

    implementation ("com.google.mlkit:text-recognition:16.0.1")


}