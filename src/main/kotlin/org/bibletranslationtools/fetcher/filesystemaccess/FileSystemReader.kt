package org.bibletranslationtools.fetcher.filesystemaccess

interface FileSystemReader {
    fun getLanguages(): List<Language>
    fun getFileTypes(): List<FileType>
    fun getBooks(language: Language, resourceType: String): List<Book>
    fun getChapters(language: Language, resourceType: String, book: Book, fileType: FileType): List<Chapter>
}
