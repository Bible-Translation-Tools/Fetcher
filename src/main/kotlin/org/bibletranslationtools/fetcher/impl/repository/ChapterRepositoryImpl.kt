package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Chapter
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.ChapterRepository
import org.bibletranslationtools.fetcher.repository.StorageAccess

class ChapterRepositoryImpl(
    private val storageAccess: StorageAccess,
    private val chapterCatalog: ChapterCatalog
) : ChapterRepository {
    override fun getChapters(
        languageCode: String,
        bookSlug: String
    ): List<Chapter> {
        val chapters = chapterCatalog.getAll(languageCode, bookSlug)
        val availableChapters = storageAccess.getChapterNumbers(languageCode, bookSlug)

        // cast it.number to string because it should NOT match if chapters are 0-padded in available chapters
        chapters.forEach {
            if (it.number.toString() in availableChapters) {
                it.availability = true
            }
        }

        return chapters
    }
}
