package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.ChapterContent
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ChapterRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess

class ChapterRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val chapterCatalog: ChapterCatalog
) : ChapterRepository {
    override fun getChaptersContent(
        languageCode: String,
        bookSlug: String,
        fileType: String
    ): List<ChapterContent> {
        val totalChapters = chapterCatalog.getChapterCount(languageCode, bookSlug)
        return storageAccess.getChaptersContent(
            languageCode,
            bookSlug,
            totalChapters,
            fileType
        )
    }
}
