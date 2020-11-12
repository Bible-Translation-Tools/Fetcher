package org.bibletranslationtools.fetcher.data.cache

data class ChapterCache(
    val number: Int,
    var availability: Boolean = false,
    var url: String? = null
)
