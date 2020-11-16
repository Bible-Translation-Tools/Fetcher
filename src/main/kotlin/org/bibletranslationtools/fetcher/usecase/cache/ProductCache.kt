package org.bibletranslationtools.fetcher.usecase.cache

class ProductCache(
    val slug: String,
    var availability: Boolean = false,
    val books: List<BookCache> = listOf()
)
