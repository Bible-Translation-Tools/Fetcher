package org.bibletranslationtools.fetcher.data.cache

data class BookCache(
    val slug: String,
    var availability: Boolean = false,
    val url: String? = null,
    val chapters: List<ChapterCache> = listOf()
)
