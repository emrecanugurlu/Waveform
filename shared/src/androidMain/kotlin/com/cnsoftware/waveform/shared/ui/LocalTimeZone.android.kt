package com.cnsoftware.waveform.shared.ui

internal actual fun localUtcOffsetMillis(epochMs: Long): Long =
    java.util.TimeZone.getDefault().getOffset(epochMs).toLong()
