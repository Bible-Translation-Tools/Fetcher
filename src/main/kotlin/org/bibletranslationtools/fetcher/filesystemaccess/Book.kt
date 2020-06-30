package org.bibletranslationtools.fetcher.filesystemaccess

data class Book(
    val id: String,
    val language: Language,
    val anglicizedName: String,
    val localizedName: String,
    val bookNumber: Int
)
