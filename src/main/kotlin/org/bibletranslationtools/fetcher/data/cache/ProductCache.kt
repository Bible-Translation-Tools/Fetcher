package org.bibletranslationtools.fetcher.data.cache

data class ProductCache(
    val slug: String,
    var availability: Boolean = false,
    val books: List<BookCache> = listOf()
)
