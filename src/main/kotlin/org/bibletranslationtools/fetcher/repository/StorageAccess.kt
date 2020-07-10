package org.bibletranslationtools.fetcher.repository

interface StorageAccess {
    fun getLanguageCodes(): List<String>
}
