package com.example.fastfilesearch.desktop.storage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Properties

/**
 * Resolves and manages local persistent storage under %LOCALAPPDATA%\SafeSearch\.
 */
object DesktopAppStorage {

    private const val APP_FOLDER_NAME = "SafeSearch"

    fun defaultAppDataDir(): File {
        val localAppData = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
            ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local")
        return File(localAppData, APP_FOLDER_NAME).apply { mkdirs() }
    }

    fun getIndexDatabaseFile(baseDir: File = defaultAppDataDir()): File {
        baseDir.mkdirs()
        return File(baseDir, "index.db")
    }

    fun getLicenseFile(baseDir: File = defaultAppDataDir()): File {
        baseDir.mkdirs()
        return File(baseDir, "license.properties")
    }

    fun getSettingsFile(baseDir: File = defaultAppDataDir()): File {
        baseDir.mkdirs()
        return File(baseDir, "settings.properties")
    }

    fun getSecurityFile(baseDir: File = defaultAppDataDir()): File {
        baseDir.mkdirs()
        return File(baseDir, "security.properties")
    }

    fun loadProperties(file: File): Properties {
        val props = Properties()
        if (file.exists() && file.isFile) {
            try {
                FileInputStream(file).use { fis ->
                    InputStreamReader(fis, StandardCharsets.UTF_8).use { reader ->
                        props.load(reader)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return props
    }

    fun saveProperties(file: File, props: Properties) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                props.store(writer, "SafeSearch Desktop Local Storage")
            }
        }
    }
}
