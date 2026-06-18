package com.onlytechtake.repairapp.domain.storage

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.io.File

class StoragePolicyTest {
    @Test
    fun requireSpaceForHeavyWriteSucceedsWhenEnoughSpace() {
        val file = mockk<File>()
        every { file.freeSpace } returns StoragePolicy.MIN_FREE_BYTES_FOR_HEAVY_WRITE + 1

        StoragePolicy.requireSpaceForHeavyWrite(file)
    }

    @Test(expected = StorageException::class)
    fun requireSpaceForHeavyWriteThrowsWhenNotEnoughSpace() {
        val file = mockk<File>()
        every { file.freeSpace } returns StoragePolicy.MIN_FREE_BYTES_FOR_HEAVY_WRITE - 1

        StoragePolicy.requireSpaceForHeavyWrite(file)
    }
}
