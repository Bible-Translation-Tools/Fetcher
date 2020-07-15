package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Chapter

interface ChapterRepository {
    fun getChapters(
        languageCode: String,
        bookSlug: String,
        fileExtension: String,
        mediaExtension: String = "",
        mediaQuality: String = ""
    ): List<Chapter>
}
