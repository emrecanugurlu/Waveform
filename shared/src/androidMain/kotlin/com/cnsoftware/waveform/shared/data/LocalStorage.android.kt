package com.cnsoftware.waveform.shared.data

import com.cnsoftware.waveform.shared.player.PlatformContextHolder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal object FileLocks {
    private val locks = HashMap<String, Any>()

    fun forPath(file: File): Any {
        val key = file.absolutePath
        return synchronized(locks) { locks.getOrPut(key) { Any() } }
    }
}

internal fun writeTextAtomically(target: File, content: String) {
    val parent = target.absoluteFile.parentFile
    if (parent != null && !parent.exists()) {
        parent.mkdirs()
    }
    val temp = File.createTempFile("${target.name}.", ".tmp", parent)
    try {
        FileOutputStream(temp).use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
        if (!temp.renameTo(target)) {
            if (!(target.exists() && target.delete() && temp.renameTo(target))) {
                throw IOException("Dosya atomik olarak kaydedilemedi: ${target.name}")
            }
        }
    } catch (t: Throwable) {
        temp.delete()
        throw t
    }
}

actual class LocalStorage actual constructor() : TextStorage {
    private val baseDir: File by lazy { PlatformContextHolder.get().filesDir }

    actual override fun readText(fileName: String): String? {
        val file = File(baseDir, fileName)
        return synchronized(FileLocks.forPath(file)) {
            if (file.exists()) file.readText() else null
        }
    }

    actual override fun writeText(fileName: String, content: String) {
        val file = File(baseDir, fileName)
        synchronized(FileLocks.forPath(file)) {
            writeTextAtomically(file, content)
        }
    }
}
