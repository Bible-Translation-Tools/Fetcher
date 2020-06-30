package org.bibletranslationtools.fetcher.filesystemaccess

data class Chapter(
    val language: Language,
    val resourceType: String,
    val book: Book,
    val chapterNumber: Int,
    val fileType: FileType
)
