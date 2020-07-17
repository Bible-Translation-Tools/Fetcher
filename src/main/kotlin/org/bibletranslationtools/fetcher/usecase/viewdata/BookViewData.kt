package org.bibletranslationtools.fetcher.usecase.viewdata

data class BookViewData(
    val index: Int,
    val slug: String,
    val anglicizedName: String,
    var availability: Boolean,
    var localizedName: String,
    val url: String
)