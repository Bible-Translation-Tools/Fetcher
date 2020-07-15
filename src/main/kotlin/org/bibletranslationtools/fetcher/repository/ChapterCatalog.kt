package org.bibletranslationtools.fetcher.repository

interface ChapterCatalog {
    fun getChapterCount(languageCode: String, bookSlug: String): Int
}
