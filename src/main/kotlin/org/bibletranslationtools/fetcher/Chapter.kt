package org.bibletranslationtools.fetcher

data class Chapter(
    val language: Language,
    val resourceType: String,
    val book: Book,
    val chapterNumber: Int,
    val fileType: FileType
) {
    fun getDownloadLink(): String {
        // TODO: use the FilePathGenerator to return a link where the grouping is 'chapter'
        return ""
    }
}
