package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Chapter

interface StorageAccess {
    fun getLanguageCodes(): List<String>
    fun getBookSlugs(languageCode: String): List<String>
    fun getChaptersWithAudio(
        languageCode: String,
        bookSlug: String,
        totalChapters: Int,
        fileType: String
    ): List<Chapter>
}
