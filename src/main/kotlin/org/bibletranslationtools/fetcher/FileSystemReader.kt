package org.bibletranslationtools.fetcher

interface FileSystemReader {

    fun getLanguages(): List<Language>

    fun getFileTypes(): List<FileTypes>

    // TODO: what books are there for a given language?
    // TODO: what chapters are there for a given language, book, and file type?
}
