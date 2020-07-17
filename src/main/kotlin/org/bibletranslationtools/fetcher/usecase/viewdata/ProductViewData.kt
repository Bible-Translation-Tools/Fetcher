package org.bibletranslationtools.fetcher.usecase.viewdata

data class ProductViewData(
    val slug: String,
    val titleKey: String,
    val descriptionKey: String,
    val iconUrl: String,
    val url: String = ""
)
