package org.bibletranslationtools.fetcher.repository

interface SourceTextAccessor {
    fun update()
    fun getRepoUrl(languageCode: String, resourceId: String): String?
}