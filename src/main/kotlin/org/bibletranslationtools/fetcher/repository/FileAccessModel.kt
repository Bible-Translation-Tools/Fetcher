package org.bibletranslationtools.fetcher.repository

data class FileAccessModel(
    val languageCode: String,
    val resourceId: String,
    val fileExtension: String,
    val bookSlug: String = "",
    val chapterNumber: String = "",
    val mediaExtension: String = "",
    val mediaQuality: String = ""
)
