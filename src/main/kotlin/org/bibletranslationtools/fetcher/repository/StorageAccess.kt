package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Chapter

interface StorageAccess {
    fun getLanguageCodes(): List<String>
    fun getBookSlugs(languageCode: String): List<String>
    fun getChapter(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        fileExtension: String,
        mediaExtension: String = "",
        mediaQuality: String = ""
    ): Chapter
}
