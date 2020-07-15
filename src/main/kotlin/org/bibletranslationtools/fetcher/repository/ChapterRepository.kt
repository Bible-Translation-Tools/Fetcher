package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Chapter

interface ChapterRepository {
    fun getChaptersWithAudio(
        languageCode: String,
        bookSlug: String,
        fileType: String
    ): List<Chapter>
}
