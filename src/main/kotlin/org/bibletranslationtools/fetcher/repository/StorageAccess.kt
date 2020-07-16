package org.bibletranslationtools.fetcher.repository

import java.io.File

interface StorageAccess {
    fun getLanguageCodes(): List<String>
    fun getBookSlugs(languageCode: String): List<String>
    fun getChapterNumbers(languageCode: String, bookSlug: String): List<String>
    fun getChapterFile(
        languageCode: String,
        bookSlug: String,
        chapterNumber: Int,
        fileExtension: String,
        mediaExtension: String = "",
        mediaQuality: String = ""
    ): File?
}
