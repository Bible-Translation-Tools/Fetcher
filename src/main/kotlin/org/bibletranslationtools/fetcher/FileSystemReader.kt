package org.bibletranslationtools.fetcher

interface FileSystemReader {
    fun getLanguages(): List<Language>
    fun getFileTypes(): List<FileType>
    fun getBooks(language: Language): List<Book>
    fun getChapters(language: Language, resourceType: String, book: Book, fileType: FileType): List<Chapter>
}
