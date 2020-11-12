package org.bibletranslationtools.fetcher.data.cache

data class LanguageCache(
    val code: String,
    var availability: Boolean = false,
    val products: List<ProductCache> = listOf()
)
