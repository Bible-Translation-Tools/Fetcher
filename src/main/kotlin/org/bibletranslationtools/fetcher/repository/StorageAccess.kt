package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Chapter

interface StorageAccess {
    fun getLanguageCodes(): List<String>
    fun getBookSlugs(languageCode: String): List<String>
    fun getChapterWithAudioFile(
        languageCode: String,
        bookSlug: String,
        chapter: String,
        fileType: String
    ): Chapter
}
