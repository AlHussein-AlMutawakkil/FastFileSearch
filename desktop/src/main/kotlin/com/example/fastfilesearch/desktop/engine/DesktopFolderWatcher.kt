package com.example.fastfilesearch.desktop.engine

import com.example.fastfilesearch.desktop.model.Workspace
import com.example.fastfilesearch.desktop.parser.DesktopFileParserDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Watches configured workspace directories on Windows using Java NIO WatchService
 * and invokes [onChangeDetected] after a short debounce whenever supported files change.
 */
class DesktopFolderWatcher(
    private val debounceMs: Long = 2500L,
    private val onChangeDetected: suspend () -> Unit
) : Closeable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchService: WatchService? = null
    private var watchJob: Job? = null
    private var debounceJob: Job? = null
    private val keyToPath = ConcurrentHashMap<WatchKey, Path>()

    @Synchronized
    fun updateWatchedWorkspaces(workspaces: List<Workspace>, enabled: Boolean) {
        stopWatching()
        if (!enabled || workspaces.isEmpty()) return

        val ws = try {
            FileSystems.getDefault().newWatchService()
        } catch (_: Throwable) {
            return
        }
        watchService = ws

        for (workspace in workspaces) {
            val dir = File(workspace.path)
            if (dir.exists() && dir.isDirectory) {
                registerDirectoryTree(ws, dir.toPath())
            }
        }

        watchJob = scope.launch {
            while (isActive) {
                val key = try {
                    ws.poll(1, TimeUnit.SECONDS)
                } catch (_: Throwable) {
                    break
                } ?: continue

                val dirPath = keyToPath[key]
                var shouldScheduleSync = false

                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    if (kind == OVERFLOW) {
                        shouldScheduleSync = true
                        continue
                    }
                    val rel = event.context() as? Path ?: continue
                    val resolved = dirPath?.resolve(rel)
                    val fileName = rel.fileName?.toString() ?: ""

                    if (kind == ENTRY_CREATE && resolved != null && Files.isDirectory(resolved)) {
                        registerDirectoryTree(ws, resolved)
                        shouldScheduleSync = true
                    } else if (kind == ENTRY_DELETE || DesktopFileParserDispatcher.isSupportedFile(fileName)) {
                        shouldScheduleSync = true
                    }
                }

                if (!key.reset()) {
                    keyToPath.remove(key)
                }

                if (shouldScheduleSync) {
                    scheduleDebouncedSync()
                }
            }
        }
    }

    private fun scheduleDebouncedSync() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounceMs)
            onChangeDetected()
        }
    }

    private fun registerDirectoryTree(ws: WatchService, root: Path) {
        try {
            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val name = dir.fileName?.toString() ?: ""
                    if (name.equals("\$RECYCLE.BIN", ignoreCase = true) ||
                        name.equals("System Volume Information", ignoreCase = true)
                    ) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    try {
                        val key = dir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                        keyToPath[key] = dir
                    } catch (_: Throwable) {
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: java.io.IOException?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    fun stopWatching() {
        debounceJob?.cancel()
        debounceJob = null
        watchJob?.cancel()
        watchJob = null
        keyToPath.clear()
        try {
            watchService?.close()
        } catch (_: Throwable) {
        }
        watchService = null
    }

    override fun close() {
        stopWatching()
        scope.cancel()
    }
}
