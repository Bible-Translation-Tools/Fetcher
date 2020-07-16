package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.ChapterContent

interface ChapterRepository {
    fun getChaptersContent(
        languageCode: String,
        bookSlug: String,
        fileType: String
    ): List<ChapterContent>
}
