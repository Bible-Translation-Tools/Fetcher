package org.bibletranslationtools.fetcher.usecase.viewdata

data class LanguageViewData(
    val code: String,
    val anglicizedName: String,
    val localizedName: String,
    val availability: Boolean,
    val url: String
)
