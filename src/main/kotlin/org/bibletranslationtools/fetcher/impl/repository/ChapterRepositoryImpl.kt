package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ChapterRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess

class ChapterRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val chapterCatalog: ChapterCatalog
) : ChapterRepository {
    override fun getChaptersWithAudio(
        languageCode: String,
        bookSlug: String,
        fileType: String
    ): List<Chapter> {
        val totalChapters = chapterCatalog.getChapterCount(languageCode, bookSlug)
        return storageAccess.getChaptersWithAudio(
            languageCode,
            bookSlug,
            totalChapters,
            fileType
        ).toList()
    }
}
