package org.bibletranslationtools.fetcher.data

import java.io.File

data class Chapter(
    val index: Int,
    val chapterFile: File?
) {
    fun hasDownload(): Boolean = chapterFile != null
}
