package org.bibletranslationtools.fetcher.repository

import java.io.File

interface StorageAccess {
    fun getLanguageCodes(): List<String>
    fun getBookSlugs(languageCode: String, resourceId: String): List<String>
    fun getChapterFile(request: FileAccessRequest?): File?
}
