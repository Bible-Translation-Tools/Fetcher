package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.repository.FileTypeCatalog
import org.bibletranslationtools.fetcher.repository.FileTypeRepository
import org.junit.Test
import org.junit.Assert.assertEquals
import org.mockito.Mockito.*

class RetrieveFileTypesTest {
    private val fileTypes = setOf("tr", "mp3")

    @Test
    fun `getFileTypes_implementationTest`() {
        val types = FileTypeRepository().getFileTypes().map { it.type }

        assertEquals(expectedFileTypes, types.toHashSet())
    }

    @Test
    fun `getFileTypes_interfaceMockingTest`() {
        val mockCatalog = mock(FileTypeCatalog::class.java)
        `when`(mockCatalog.getFileTypes()).thenReturn(FileTypeRepository().getFileTypes())

        val types = mockCatalog.getFileTypes().map { it.type }
        verify(mockCatalog).getFileTypes()

        assertEquals(expectedFileTypes, types.toHashSet())
    }
}