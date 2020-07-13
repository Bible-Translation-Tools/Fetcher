package org.bibletranslationtools.fetcher.data

data class Book(
    val index: Int,
    val slug: String,
    val anglicizedName: String,
    var availability: Boolean = false,
    val localizedName: String = ""
)
