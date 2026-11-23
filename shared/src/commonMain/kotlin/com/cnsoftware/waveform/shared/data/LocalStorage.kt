package com.cnsoftware.waveform.shared.data

interface TextStorage {
    fun readText(fileName: String): String?
    fun writeText(fileName: String, content: String)
}

expect class LocalStorage() : TextStorage {
    override fun readText(fileName: String): String?
    override fun writeText(fileName: String, content: String)
}
