package org.bibletranslationtools.fetcher

import junit.framework.Assert.assertEquals
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.domain.DirectoryProvider
import org.bibletranslationtools.fetcher.domain.SourceAudioContent
import org.bibletranslationtools.fetcher.repository.LanguageCatalog
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.io.File
import java.io.FileFilter

class SourceAudioContentTest {

    @Test
    fun dirProvider() {
        val mockDirectoryProvider = Mockito.mock(DirectoryProvider::class.java)
        val mockFile = Mockito.mock(File::class.java)

        `when`(mockFile.listFiles(any(FileFilter::class.java)))
            .thenReturn(arrayOf(
                File("/SourceAudio/en"), File("/SourceAudio/lo"), File("/SourceAudio/ja")
            ))
        `when`(mockDirectoryProvider.getSourceAudioRoot())
            .thenReturn(mockFile)

        val mockCatalog = Mockito.mock(LanguageCatalog::class.java)

        `when`(mockCatalog.getLanguages())
            .thenReturn(listOf(
                Language("en", "English", "English"),
                Language("lo", "Laotian", "ພາສາລາວ"),
                Language("pmy", "Papuan Malay", "Papuan Malay")
            ))

        val saContent = SourceAudioContent(mockCatalog, mockDirectoryProvider)
        assertEquals(
            listOf(
                Language("en", "English", "English"),
                Language("lo", "Laotian", "ພາສາລາວ")
            ),
            saContent.getLanguages()
        )
    }

}