import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Strictly offline local database, document parsers, and Excel exporter
    implementation(libs.sqlite.jdbc)
    implementation(libs.poi.ooxml)
    implementation(libs.poi.scratchpad)
    implementation(libs.pdfbox.jvm)
    implementation(libs.jackcess)

    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.example.fastfilesearch.desktop.MainKt"
        jvmArgs("-Dfile.encoding=UTF-8", "-Xmx1024m")

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "SafeSearch"
            packageVersion = "6.0.0"
            description = "بحث آمن - محرك بحث ومفهرس ملفات محلي لسطح المكتب"
            vendor = "FastFileSearch"
            includeAllModules = true

            windows {
                menuGroup = "بحث آمن"
                shortcut = true
                perUserInstall = true
                upgradeUuid = "8f4b2a9d-3e1c-4c7f-9b2a-5d6e7f8a9b0c"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
    }
}
