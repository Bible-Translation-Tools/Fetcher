package org.bibletranslationtools.fetcher.usecase.viewdata

data class ChapterViewData(
    val chapterNumber: Int,
    val url: String?
) {
    val titleKey: String = "chapter"   // localization key
}
