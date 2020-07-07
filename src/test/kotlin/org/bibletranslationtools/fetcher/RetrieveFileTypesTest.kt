package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.repository.FileTypeRepository
import org.junit.Test
import org.junit.Assert.assertEquals
class RetrieveFileTypesTest {
    private val fileTypes = setOf("tr", "mp3")

    @Test
    fun `getSupportedFileTypes`() {
        val types = FileTypeRepository().getFileTypes().map { it.type }
        assertEquals(fileTypes, types.toHashSet())
    }
}