package org.bibletranslationtools.fetcher

interface FileSystemReader {

    fun getLanguages(): List<Language>

    // TODO: what file types are supported?
    // TODO: what books are there for a given language?
    // TODO: what chapters are there for a given language, book, and file type?
}