plugins {
    kotlin("multiplatform")
    id("com.android.library")
}
kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
android {
    namespace = "com.blockpuzzle.game"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
}
