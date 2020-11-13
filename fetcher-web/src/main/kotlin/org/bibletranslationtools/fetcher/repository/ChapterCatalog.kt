package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Chapter

interface ChapterCatalog {
    fun getAll(languageCode: String, bookSlug: String): List<Chapter>
}
