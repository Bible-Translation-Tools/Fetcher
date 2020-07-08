package org.bibletranslationtools.fetcher

import org.bibletranslationtools.fetcher.data.FileType
import org.bibletranslationtools.fetcher.repository.FileTypeCatalog
import org.bibletranslationtools.fetcher.repository.FileTypeRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class RetrieveFileTypesTest {
    private val expectedFileTypes = listOf(
        FileType("tr", "", "", ""),
        FileType("mp3", "", "", "")
    )

    @Test
    fun `getFileTypes_withMock`() {
        val mockCatalog = mock(FileTypeCatalog::class.java)
        `when`(mockCatalog.getFileTypes()).thenReturn(expectedFileTypes)

        val types = mockCatalog.getFileTypes().map { it.type }
        val actualTypes = FileTypeRepository().getFileTypes().map { it.type }

        assertEquals(types.toHashSet(), actualTypes.toHashSet())
    }
}
