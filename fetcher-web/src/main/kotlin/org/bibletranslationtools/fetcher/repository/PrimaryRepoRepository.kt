package org.bibletranslationtools.fetcher.repository

interface PrimaryRepoRepository {
    fun fetch(languageCode: String, resourceType: String): String?
}