package org.bibletranslationtools.fetcher.usecase.cache

class AvailabilityCache(
    var languages: List<LanguageCache>
)

class LanguageCache(
    val code: String,
    var availability: Boolean = false,
    val products: List<ProductCache> = listOf()
)

class ProductCache(
    val slug: String,
    var availability: Boolean = false,
    val books: List<BookCache> = listOf()
)

class BookCache(
    val slug: String,
    var availability: Boolean = false,
    val url: String? = null,
    val chapters: List<ChapterCache> = listOf()
)

data class ChapterCache(
    val number: Int,
    var availability: Boolean = false,
    var url: String? = null
)
