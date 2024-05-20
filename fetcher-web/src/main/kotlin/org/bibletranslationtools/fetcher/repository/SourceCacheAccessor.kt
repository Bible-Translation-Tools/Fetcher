package org.bibletranslationtools.fetcher.repository

interface SourceCacheAccessor {
    fun update()
    fun getRepoUrl(languageCode: String, resourceId: String): String?
}