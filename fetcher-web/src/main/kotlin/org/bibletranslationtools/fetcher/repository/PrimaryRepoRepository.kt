package org.bibletranslationtools.fetcher.repository

interface PrimaryRepoRepository {
    fun getRepoUrl(languageCode: String, resourceType: String): String?
}