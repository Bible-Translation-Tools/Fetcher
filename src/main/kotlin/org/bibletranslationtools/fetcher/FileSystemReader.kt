package org.bibletranslationtools.fetcher

interface FileSystemReader {
    fun getLanguages(): List<Language>
    fun getFileTypes(): List<FileType>
    fun getBooks(language: Language): List<Book>
    // TODO: what chapters are there for a given language, book, and file type?
}
