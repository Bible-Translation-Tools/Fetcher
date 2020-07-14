package org.bibletranslationtools.fetcher.repository

interface StorageAccess {
    fun getLanguageCodes(): List<String>
    fun getBookSlugs(languageCode: String): List<String>
}
