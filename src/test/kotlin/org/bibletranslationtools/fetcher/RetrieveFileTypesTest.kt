package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.repository.FileTypeRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class RetrieveFileTypesTest {
    enum class AvailableFileType(val type: String) {
        MP3("mp3"),
        TR("tr")
    }

    @Test
    fun `testGetFileTypes`() {
        val expectedFileTypes = AvailableFileType.values().map { it.type }
        val actualTypes = FileTypeRepository().getFileTypes().map { it.type }

        assertEquals(expectedFileTypes.toSet(), actualTypes.toSet())
    }
}
