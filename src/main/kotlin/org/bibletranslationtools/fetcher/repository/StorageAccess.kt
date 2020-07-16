package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.ChapterContent

interface StorageAccess {
    fun getLanguageCodes(): List<String>
    fun getBookSlugs(languageCode: String): List<String>
    fun getChaptersContent(
        languageCode: String,
        bookSlug: String,
        totalChapters: Int,
        fileType: String
    ): List<ChapterContent>
}
