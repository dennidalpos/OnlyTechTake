package com.onlytechtake.repairapp.domain.storage

import java.io.File

object StoragePolicy {
    // 100 MiB for safety during heavy operations (backups, imports, PDF generation)
    const val MIN_FREE_BYTES_FOR_HEAVY_WRITE = 100 * 1024 * 1024L
    
    // 50 MiB threshold for critical storage warnings
    const val CRITICAL_FREE_BYTES = 50 * 1024 * 1024L
    
    // 10 MiB threshold for blocking ALL writes if possible
    const val BLOCKING_FREE_BYTES = 10 * 1024 * 1024L

    fun requireSpaceForHeavyWrite(filesDir: File) {
        val free = filesDir.freeSpace
        if (free < MIN_FREE_BYTES_FOR_HEAVY_WRITE) {
            throw StorageException("Spazio insufficiente sul dispositivo per operazioni pesanti. Libera almeno 100 MB.")
        }
    }
}

class StorageException(message: String) : Exception(message)
