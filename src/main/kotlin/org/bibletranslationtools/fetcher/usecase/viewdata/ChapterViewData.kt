package org.bibletranslationtools.fetcher.usecase.viewdata

data class ChapterViewData(
    val chapterNumber: Int,
    val url: String?
) {
    val titleKey: String = "chapter" // localization key
    val downloadFileName: String = if (url != null) java.io.File(url).name else ""
}
