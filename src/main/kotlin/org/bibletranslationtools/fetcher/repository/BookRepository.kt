package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Language

interface BookRepository {
    fun getBooks(languageCode: String): List<Book>
    fun getBooks(language: Language): List<Book>
    fun getBook(slug: String, languageCode: String = "en"): Book?
    fun getBook(slug: String, language: Language): Book?
}
