package org.bibletranslationtools.fetcher.repository

data class FileAccessRequest(
    val languageCode: String,
    val resourceId: String,
    val fileExtension: String = "",
    val bookSlug: String = "",
    val chapter: String = "",
    val mediaExtension: String = "",
    val mediaQuality: String = ""
)
