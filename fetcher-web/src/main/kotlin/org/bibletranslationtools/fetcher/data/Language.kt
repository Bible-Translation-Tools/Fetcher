package org.bibletranslationtools.fetcher.data

data class Language(
    val code: String,
    val anglicizedName: String,
    val localizedName: String,
    var availability: Boolean = false
)
