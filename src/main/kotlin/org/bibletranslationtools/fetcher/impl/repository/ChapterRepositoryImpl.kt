package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.data.Language
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ChapterRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess

class ChapterRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val chapterCatalog: ChapterCatalog
): ChapterRepository {
    override fun getChapters(
        languageCode: String,
        bookSlug: String,
        fileExtension: String,
        mediaExtension: String,
        mediaQuality: String
    ): List<Chapter> {
        val totalChapters = chapterCatalog.getChapterCount(languageCode, bookSlug)

        val chapterList = mutableListOf<Chapter>()
        for(chapterNumber in 1..totalChapters) {
            chapterList.add(storageAccess.getChapter(
                languageCode,
                bookSlug,
                chapterNumber,
                fileExtension,
                mediaExtension,
                mediaQuality
            ))
        }

        return chapterList.toList()
    }
}
