package org.bibletranslationtools.fetcher.repository

data class FileAccessModel(
    val languageCode: String,
    val resourceId: String,
    val bookSlug: String,
    val chapterNumber: Int,
    val fileExtension: String,
    val mediaExtension: String = "",
    val mediaQuality: String = ""
)
