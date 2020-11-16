package org.bibletranslationtools.fetcher.usecase.cache

class LanguageCache(
    val code: String,
    var availability: Boolean = false,
    val products: List<ProductCache> = listOf()
)
