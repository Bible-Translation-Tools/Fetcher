package org.bibletranslationtools.fetcher.usecase.viewdata

data class ChapterViewData(
    val chapterNumber: Int,
    val titleKey: String,   // localization key
    val url: String
)