package com.cnsoftware.waveform.shared.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLock
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

private val storageLock = NSLock()

private inline fun <T> withStorageLock(block: () -> T): T {
    storageLock.lock()
    return try {
        block()
    } finally {
        storageLock.unlock()
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class LocalStorage actual constructor() : TextStorage {
    private val documentsDir: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        paths.firstOrNull()?.toString() ?: ""
    }

    actual override fun readText(fileName: String): String? = withStorageLock {
        if (documentsDir.isEmpty()) return@withStorageLock null
        val path = "$documentsDir/$fileName"
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return@withStorageLock null
        NSString.stringWithContentsOfFile(
            path = path,
            encoding = NSUTF8StringEncoding,
            error = null
        )
    }

    actual override fun writeText(fileName: String, content: String) = withStorageLock {
        if (documentsDir.isEmpty()) return@withStorageLock
        val path = "$documentsDir/$fileName"
        NSString.create(string = content).writeToFile(
            path = path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        Unit
    }
}
