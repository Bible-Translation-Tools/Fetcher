package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Book
import org.bibletranslationtools.fetcher.data.Language

interface BookRepository {
    fun getBooks(resourceId: String, languageCode: String = "en"): List<Book> // default to English
    fun getBooks(language: Language, resourceId: String): List<Book>
    fun getBook(slug: String, languageCode: String = "en"): Book? // default to English
    fun getBook(slug: String, language: Language): Book?
}
