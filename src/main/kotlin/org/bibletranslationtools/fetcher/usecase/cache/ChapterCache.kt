package org.bibletranslationtools.fetcher.usecase.cache

data class ChapterCache(
    val number: Int,
    var availability: Boolean = false,
    var url: String? = null
)
