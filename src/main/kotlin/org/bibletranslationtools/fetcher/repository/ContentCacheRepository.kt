package org.bibletranslationtools.fetcher.repository

interface ContentCacheRepository {
    fun update()
    fun isLanguageAvailable(code: String): Boolean
    fun isProductAvailable(productSlug: String, languageCode: String): Boolean

    fun isBookAvailable(
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): Boolean

    fun isChapterAvailable(
        number: Int,
        bookSlug: String,
        languageCode: String,
        productSlug: String
    ): Boolean
}
